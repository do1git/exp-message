import { SyntheticEvent, useState } from 'react';
import { useAuthStore } from './store';

export default function AuthView() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [signupMode, setSignupMode] = useState(false);

    const authStatus = useAuthStore((state) => state.authStatus);
    const errorMessage = useAuthStore((state) => state.errorMessage);
    const login = useAuthStore((state) => state.login);
    const signupAndLogin = useAuthStore((state) => state.signupAndLogin);

    const isLoading = authStatus === 'loading';

    async function handleSubmit(event: SyntheticEvent) {
        event.preventDefault();
        if (!email.trim() || !password.trim()) {
            return;
        }

        if (signupMode) {
            await signupAndLogin(email.trim(), password);
            return;
        }

        await login(email.trim(), password);
    }

    return (
        <div className="flex-1 bg-gray-50 p-4">
            <div className="rounded-xl bg-white p-4 shadow-sm">
                <h3 className="text-base font-semibold text-gray-900">Sign in to continue</h3>
                <p className="mt-1 text-sm text-gray-600">Use your email and quick password.</p>

                <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
                    <label className="block">
                        <span className="mb-1 block text-xs font-medium text-gray-600">Email</span>
                        <input
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="name@company.com"
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 outline-none transition focus:border-blue-500"
                            autoComplete="email"
                            required
                        />
                    </label>

                    <label className="block">
                        <span className="mb-1 block text-xs font-medium text-gray-600">Password</span>
                        <input
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 outline-none transition focus:border-blue-500"
                            autoComplete={signupMode ? 'new-password' : 'current-password'}
                            maxLength={20}
                            required
                        />
                    </label>

                    {errorMessage && <p className="text-xs text-red-600">{errorMessage}</p>}

                    <button
                        type="submit"
                        className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:bg-blue-300"
                        disabled={isLoading}
                    >
                        {isLoading ? 'Please wait...' : signupMode ? 'Create account' : 'Login'}
                    </button>
                </form>

                <div className="mt-4 text-center text-sm text-gray-500">
                    First time?{' '}
                    <button
                        type="button"
                        className="font-medium text-blue-600 hover:text-blue-700"
                        onClick={() => setSignupMode((prev) => !prev)}
                        disabled={isLoading}
                    >
                        {signupMode ? 'Back to login' : 'Sign up'}
                    </button>
                </div>
            </div>
        </div>
    );
}
