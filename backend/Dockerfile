FROM python:3.12

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

WORKDIR /app

COPY . /app/
RUN pip install --no-cache-dir ".[dev]"

EXPOSE 8000

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]