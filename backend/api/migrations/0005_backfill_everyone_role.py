from django.db import migrations


def backfill_everyone_roles(apps, schema_editor):
    Guild = apps.get_model("api", "Guild")
    Role = apps.get_model("api", "Role")

    # VIEW_CHANNEL (16) | SEND_MESSAGES (32) = 48
    DEFAULT_PERMISSIONS = 48

    for guild in Guild.objects.all():
        if not Role.objects.filter(guild=guild, is_everyone=True).exists():
            Role.objects.create(
                guild=guild,
                name="@everyone",
                permissions=DEFAULT_PERMISSIONS,
                position=0,
                is_everyone=True,
            )


def reverse_backfill(apps, schema_editor):
    pass


class Migration(migrations.Migration):

    dependencies = [
        ("api", "0004_role_guildmemberrole_channelpermissionoverwrite_and_more"),
    ]

    operations = [
        migrations.RunPython(backfill_everyone_roles, reverse_backfill),
    ]
