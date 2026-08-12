import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  use: { baseURL: 'http://127.0.0.1:5173', channel: 'chrome', trace: 'retain-on-failure' },
  webServer: [
    { command: 'mvn spring-boot:run', cwd: '../backend', url: 'http://127.0.0.1:8080/swagger-ui/index.html', reuseExistingServer: true, timeout: 120_000 },
    { command: 'npm run dev -- --host 127.0.0.1', cwd: '.', url: 'http://127.0.0.1:5173/login', reuseExistingServer: true, timeout: 120_000 }
  ]
})
