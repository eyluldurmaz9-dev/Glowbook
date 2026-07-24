import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../providers.dart';
import '../widgets/shared_widgets.dart';

class RegisterPage extends ConsumerStatefulWidget {
  const RegisterPage({super.key});

  @override
  ConsumerState<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends ConsumerState<RegisterPage> {
  final _ctrlUser = TextEditingController();
  final _ctrlPass = TextEditingController();

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authStateProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Register')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            AppTextField(controller: _ctrlUser, label: 'Username'),
            const SizedBox(height: 12),
            AppTextField(
                controller: _ctrlPass, label: 'Password', obscure: true),
            const SizedBox(height: 24),
            AppButton(
              label: auth.isLoading ? 'Loading...' : 'Register',
              onPressed: auth.isLoading
                  ? null
                  : () async {
                      final payload = {
                        'username': _ctrlUser.text.trim(),
                        'password': _ctrlPass.text.trim()
                      };
                      final resp =
                          await ref.read(authServiceProvider).register(payload);
                      if (resp['success'] == true || resp['data'] != null) {
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Registered')));
                        context.go('/login');
                      } else {
                        ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Register failed')));
                      }
                    },
            ),
          ],
        ),
      ),
    );
  }
}
