import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers.dart';
import '../widgets/shared_widgets.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _phoneCtrl = TextEditingController();
  final _passCtrl = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authStateProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Login')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            AppTextField(controller: _phoneCtrl, label: 'Phone'),
            const SizedBox(height: 12),
            AppTextField(
                controller: _passCtrl, label: 'Password', obscure: true),
            const SizedBox(height: 24),
            AppButton(
              label: auth.isLoading ? 'Loading...' : 'Login',
              onPressed: auth.isLoading
                  ? null
                  : () async {
                      final ok = await ref
                          .read(authStateProvider.notifier)
                          .login(_phoneCtrl.text.trim(), _passCtrl.text.trim());
                      if (ok) {
                        context.go('/home');
                      } else {
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Login failed')));
                      }
                    },
            ),
            TextButton(
              onPressed: () => context.go('/register'),
              child: const Text('Register'),
            )
          ],
        ),
      ),
    );
  }
}
