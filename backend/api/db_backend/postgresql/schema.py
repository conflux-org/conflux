from django.db.backends.ddl_references import ForeignKeyName
from django.db.backends.postgresql.schema import (
    DatabaseSchemaEditor as PostgresDatabaseSchemaEditor,
)


class DatabaseSchemaEditor(PostgresDatabaseSchemaEditor):
    def _create_index_name(self, table_name, column_names, suffix=""):
        from django.db.backends.base.schema import split_identifier

        _, clean_table_name = split_identifier(table_name)
        clean_columns = "_".join(column_names)

        if suffix == "_uniq" or suffix == "uniq":
            index_name = f"uk_{clean_table_name}_{clean_columns}"
        else:
            index_name = f"idx_{clean_table_name}_{clean_columns}"
            if suffix and suffix not in ("_idx", "idx"):
                index_name = f"{index_name}{suffix}"

        max_length = self.connection.ops.max_name_length() or 63
        if len(index_name) > max_length:
            from django.db.backends.base.schema import names_digest

            hash_part = names_digest(clean_table_name, *column_names, length=6)
            prefix = index_name[: max_length - 8]
            index_name = f"{prefix}_{hash_part}"

        return index_name

    def _fk_constraint_name(self, model, field, suffix):
        source_table = model._meta.db_table
        from django.db.backends.base.schema import split_identifier

        _, clean_source_table = split_identifier(source_table)

        target_table = field.target_field.model._meta.db_table
        _, clean_target_table = split_identifier(target_table)

        column_name = field.column
        fk_name = f"fk_{clean_source_table}_{clean_target_table}_{column_name}"

        max_length = self.connection.ops.max_name_length() or 63
        if len(fk_name) > max_length:
            from django.db.backends.base.schema import names_digest

            hash_part = names_digest(
                clean_source_table, clean_target_table, column_name, length=6
            )
            prefix = fk_name[: max_length - 8]
            fk_name = f"{prefix}_{hash_part}"

        def create_fk_name(*args, **kwargs):
            return self.quote_name(fk_name)

        return ForeignKeyName(
            source_table,
            [field.column],
            clean_target_table,
            [field.target_field.column],
            suffix,
            create_fk_name,
        )
