import axios from "axios";

const AUTH_URL = import.meta.env.VITE_AUTH_API; // https://adminapidev.ipca.com

export const authApi = axios.create({
    baseURL: AUTH_URL,
    withCredentials: false,
});

authApi.interceptors.request.use((config) => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export interface LoginResponse {
    token: string;
    userId: string;
    firstName: string;
    lastName: string;
}

export const login = async (
    userId: string,
    password: string
): Promise<LoginResponse> => {
    try {
        const response = await authApi.post<LoginResponse>("/auth/login", {
            userId,
            password,
            applicationName: "DMS 2.0",
        });
        localStorage.setItem("jwtToken", response.data.token);
        localStorage.setItem("userId", response.data.userId);
        return response.data;
    } catch (error: any) {
        throw new Error(
            error.response?.data?.message || "Invalid credentials"
        );
    }
};