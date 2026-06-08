"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { authApi } from "../endpoints";
import { useAuthStore } from "@/stores/useAuthStore";
import type { LoginRequest, LoginResponse } from "../types";

export function useLogin() {
  const login = useAuthStore((s) => s.login);

  return useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (res: unknown) => {
      const loginRes = res as LoginResponse;
      const { user } = loginRes;
      login(
        {
          id: user.id,
          username: user.username,
          displayName: user.displayName,
          email: user.email,
          avatar: user.avatar,
          roles: user.roleNames,
        },
        loginRes.accessToken,
        loginRes.refreshToken
      );
    },
  });
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout);

  return useMutation({
    mutationFn: () => authApi.logout(),
    onSettled: () => logout(),
  });
}

export function useCurrentUser() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authApi.me(),
    enabled: useAuthStore((s) => !!s.accessToken),
  });
}
