import { defineConfig, devices } from '@playwright/test'
import { defineBddConfig } from 'playwright-bdd'

export default defineConfig({
  testDir: defineBddConfig({ features: 'bdd/*.feature', steps: 'bdd/*.steps.ts' }),
  forbidOnly: !!process.env.CI,
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-bdd-report', open: 'never' }]],
  outputDir: 'test-results-bdd',
  use: { baseURL: 'http://localhost:15174', trace: 'retain-on-failure' },
  projects: [{ name: 'chrome', use: { ...devices['Desktop Chrome'], channel: 'chrome' } }],
  webServer: {
    command: 'npm run dev -- --host localhost --port 15174 --strictPort',
    url: 'http://localhost:15174',
    env: { VITE_API_URL: 'http://localhost:15174' },
    reuseExistingServer: false,
  },
})
