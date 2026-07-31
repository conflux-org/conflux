import contextvars

from django.db import models, transaction
from django.db.models.deletion import Collector
from django.utils import timezone

_soft_delete_active = contextvars.ContextVar("soft_delete_active", default=True)


class SoftDeleteQuerySet(models.QuerySet):
    def delete(self):
        if _soft_delete_active.get():
            using = self.db
            collector = Collector(using=using, origin=self)
            collector.collect(self)
            return SoftDeleteModel._perform_soft_delete_for_collector(collector, using)
        else:
            return super().delete()

    def hard_delete(self):
        token = _soft_delete_active.set(False)
        try:
            return self.delete()
        finally:
            _soft_delete_active.reset(token)


class SoftDeleteManager(models.Manager):
    def get_queryset(self):
        return SoftDeleteQuerySet(self.model, using=self._db).filter(
            deleted_at__isnull=True
        )


class AllObjectsSoftDeleteManager(models.Manager):
    def get_queryset(self):
        return SoftDeleteQuerySet(self.model, using=self._db)


def prevent_fast_delete(sender, instance, **kwargs):
    pass


class SoftDeleteModel(models.Model):
    deleted_at = models.DateTimeField(null=True, blank=True, db_index=True)

    objects = SoftDeleteManager()
    all_objects = AllObjectsSoftDeleteManager()

    class Meta:
        abstract = True

    def delete(self, using=None, keep_parents=False):
        if _soft_delete_active.get():
            using = using or self._state.db
            collector = Collector(using=using)
            collector.collect([self], keep_parents=keep_parents)
            return self._perform_soft_delete_for_collector(collector, using)
        else:
            return super().delete(using=using, keep_parents=keep_parents)

    def hard_delete(self, using=None, keep_parents=False):
        token = _soft_delete_active.set(False)
        try:
            return self.delete(using=using, keep_parents=keep_parents)
        finally:
            _soft_delete_active.reset(token)

    @classmethod
    def _perform_soft_delete_for_collector(cls, collector, using):
        now = timezone.now()
        deleted_counter = {}

        with transaction.atomic(using=using):
            for model, instances in collector.data.items():
                count = len(instances)
                if count == 0:
                    continue

                model_label = model._meta.label
                deleted_counter[model_label] = (
                    deleted_counter.get(model_label, 0) + count
                )

                if issubclass(model, SoftDeleteModel):
                    pk_list = [obj.pk for obj in instances]
                    model.all_objects.filter(
                        pk__in=pk_list, deleted_at__isnull=True
                    ).update(deleted_at=now)
                    for obj in instances:
                        if obj.deleted_at is None:
                            obj.deleted_at = now
                else:
                    sub_collector = Collector(using=using)
                    sub_collector.data = {model: instances}
                    sub_collector.delete()

            for (model, field), (value, objs) in collector.field_updates.items():
                if isinstance(objs, models.QuerySet):
                    objs.update(**{field.name: value})
                else:
                    pk_list = [obj.pk for obj in objs]
                    model.all_objects.filter(pk__in=pk_list).update(
                        **{field.name: value}
                    )

        total_deleted = sum(deleted_counter.values())
        return total_deleted, deleted_counter


class User(SoftDeleteModel):
    name = models.CharField(max_length=255)
    password = models.CharField(max_length=255, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "users"

    def __str__(self):
        return self.name


class Guild(SoftDeleteModel):
    name = models.CharField(max_length=255)
    owner = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="owned_guilds",
        db_column="owner_id",
    )
    members = models.ManyToManyField(
        User,
        related_name="guilds",
        through="GuildMember",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "guilds"

    def __str__(self):
        return self.name


class GuildMember(SoftDeleteModel):
    guild = models.ForeignKey(
        Guild,
        on_delete=models.CASCADE,
        db_column="guild_id",
    )
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        db_column="user_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "guild_members"
        constraints = (
            models.UniqueConstraint(
                fields=["guild", "user"],
                condition=models.Q(deleted_at__isnull=True),
                name="uk_guild_members_guild_id_user_id",
            ),
        )

    def __str__(self):
        return f"{self.guild_id} - {self.user_id}"


class Channel(SoftDeleteModel):
    name = models.CharField(max_length=255)
    guild = models.ForeignKey(
        Guild,
        on_delete=models.CASCADE,
        related_name="channels",
        db_column="guild_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "channels"

    def __str__(self):
        return self.name


class Message(SoftDeleteModel):
    author = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="messages",
        db_column="author_id",
    )
    content = models.TextField()
    channel = models.ForeignKey(
        Channel,
        on_delete=models.CASCADE,
        related_name="messages",
        db_column="channel_id",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = "messages"

    def __str__(self):
        return f"Message({self.id}) in Channel({self.channel_id}) by User({self.author_id})"
