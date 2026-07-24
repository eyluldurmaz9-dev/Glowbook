import 'package:flutter/foundation.dart';
import 'package:go_router/go_router.dart';
import '../pages/splash_page.dart';
import '../pages/login_page.dart';
import '../pages/register_page.dart';
import '../pages/home_page.dart';
import '../pages/onboarding_page.dart';
import '../pages/services_page.dart';
import '../pages/packages_page.dart';
import '../pages/appointments_page.dart';
import '../pages/notifications_page.dart';
import '../pages/profile_page.dart';
import '../pages/settings_page.dart';
import '../state/auth_state.dart';

final _routes = [
  GoRoute(path: '/', builder: (context, state) => const SplashPage()),
  GoRoute(
      path: '/onboarding', builder: (context, state) => const OnboardingPage()),
  GoRoute(path: '/login', builder: (context, state) => const LoginPage()),
  GoRoute(path: '/register', builder: (context, state) => const RegisterPage()),
  GoRoute(path: '/home', builder: (context, state) => const HomePage()),
  GoRoute(path: '/services', builder: (context, state) => const ServicesPage()),
  GoRoute(path: '/packages', builder: (context, state) => const PackagesPage()),
  GoRoute(
      path: '/appointments',
      builder: (context, state) => const AppointmentsPage()),
  GoRoute(
      path: '/notifications',
      builder: (context, state) => const NotificationsPage()),
  GoRoute(path: '/profile', builder: (context, state) => const ProfilePage()),
  GoRoute(path: '/settings', builder: (context, state) => const SettingsPage()),
];

GoRouter createAppRouter(AuthState authState, Listenable refreshListenable) {
  return GoRouter(
    initialLocation: '/',
    refreshListenable: refreshListenable,
    routes: _routes,
    redirect: (context, state) {
      final location = state.location;
      final loggingIn = location == '/login' ||
          location == '/register' ||
          location == '/onboarding';
      if (!authState.isAuthenticated && !loggingIn && location != '/') {
        return '/login';
      }
      if (authState.isAuthenticated &&
          (location == '/login' ||
              location == '/register' ||
              location == '/')) {
        return '/home';
      }
      return null;
    },
  );
}
