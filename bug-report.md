# 🐞 Bug Report – Script Injection Not Handled in Email Body

- **Title**: Email body accepts raw HTML/JS
- **Severity**: Medium
- **Steps to Reproduce**:
  1. POST to `/send-email` with:
  ```json
  {
    "to": "user@example.com",
    "subject": "Test",
    "body": "<script>alert('XSS')</script>"
  }
  ```
  2. Observe server response.
- **Expected**: Should sanitize or reject script injection.
- **Actual**: Server returns `202 Accepted`.
- **Notes**: Potential XSS risk if rendered without sanitization downstream.
