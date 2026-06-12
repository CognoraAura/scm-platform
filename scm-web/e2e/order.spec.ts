import { test, expect } from '@playwright/test';

test('create order flow', async ({ page }) => {
  // Login first
  await page.goto('/login');
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.click('button[type="submit"]');

  // Navigate to orders
  await page.goto('/order');
  await expect(page.locator('h1')).toContainText('Orders');

  // Create new order
  await page.click('button:has-text("New Order")');
  await page.fill('input[name="customer"]', 'Test Customer');
  await page.click('button:has-text("Submit")');

  // Verify order created
  await expect(page.locator('.success-message')).toBeVisible();
});
