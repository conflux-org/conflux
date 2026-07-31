from django.apps import AppConfig


class ApiConfig(AppConfig):
    name = "api"

    def ready(self):
        from django.db.models.signals import pre_delete

        from api.models import SoftDeleteModel, prevent_fast_delete

        for model in self.get_models():
            if issubclass(model, SoftDeleteModel):
                pre_delete.connect(prevent_fast_delete, sender=model, weak=False)
