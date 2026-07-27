FROM python:3.12

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

WORKDIR /app

# 正確：從根目錄出發，指定 Dockerfile 資料夾內的 requirements.txt
COPY Dockerfile/requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

# 正確：複製整個 backend 專案內容到容器中
COPY . /app/

EXPOSE 8000

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]