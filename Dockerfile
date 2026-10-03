FROM python:3.11-slim
WORKDIR /app
COPY app.py .
EXPOSE 3000 5000
ENV PYTHONUNBUFFERED=1
CMD ["python3", "app.py"]
