# 프론트엔드 롤백 방법

## 1. main.jsp 롤백
```javascript
// 기존 fetch wrapper로 롤백
window.apiFetch = function(url, options = {}) {
  const defaultOptions = {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      ...options.headers
    }
  };
  return fetch(url, { ...defaultOptions, ...options });
};

// fetch 전역 오버라이드 제거
// const originalFetch = window.fetch; 부분 삭제

// axios 설정 단순화
if (typeof axios !== 'undefined') {
  axios.defaults.withCredentials = true;
  axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';
  window.apiAxios = axios;
}
```

## 2. 롤백 명령어
```bash
# Git을 사용하는 경우
git checkout HEAD~1 -- src/main/webapp/WEB-INF/jsp/main/main.jsp

# 또는 백업 파일에서 복원
copy backup\main_backup.jsp src\main\webapp\WEB-INF\jsp\main\main.jsp
```
