import { theme } from 'antd'
import type { ThemeConfig } from 'antd'
import { tokens } from './design-tokens'

export const lightTheme: ThemeConfig = {
  token: {
    colorPrimary: tokens.colors.primary,
    colorSuccess: tokens.colors.success,
    colorWarning: tokens.colors.warning,
    colorError: tokens.colors.error,
    borderRadius: tokens.radius.base,
    fontSize: tokens.fontSize.base,
  },
  algorithm: theme.defaultAlgorithm,
}

export const darkTheme: ThemeConfig = {
  token: {
    colorPrimary: '#1668dc',
    colorSuccess: '#49aa19',
    colorWarning: '#d89614',
    colorError: '#dc4446',
    borderRadius: tokens.radius.base,
    fontSize: tokens.fontSize.base,
    colorBgContainer: '#141414',
    colorBgLayout: '#000000',
    colorBgElevated: '#1f1f1f',
    colorText: 'rgba(255,255,255,0.88)',
    colorTextSecondary: 'rgba(255,255,255,0.65)',
    colorBorder: '#424242',
    colorBorderSecondary: '#303030',
  },
  algorithm: theme.darkAlgorithm,
}
