from django.test import TestCase
from django.urls import reverse

# Create your tests here.
class APITestCase(TestCase):
    def test_api_returns_ok_response(self):
        url = reverse('test_api')
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {
            "status": "ok",
            "message": "API test successful"
        })

