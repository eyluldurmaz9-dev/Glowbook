import 'package:flutter/material.dart';

class AppButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool primary;

  const AppButton(
      {super.key, required this.label, this.onPressed, this.primary = true});

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: primary ? Colors.pink : Colors.grey[200],
        foregroundColor: primary ? Colors.white : Colors.black,
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 20),
      ),
      child: Text(label),
    );
  }
}

class AppTextField extends StatelessWidget {
  final TextEditingController controller;
  final String label;
  final bool obscure;

  const AppTextField(
      {super.key,
      required this.controller,
      required this.label,
      this.obscure = false});

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      obscureText: obscure,
      decoration:
          InputDecoration(labelText: label, border: const OutlineInputBorder()),
    );
  }
}

class AppCard extends StatelessWidget {
  final Widget child;

  const AppCard({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 0),
      child: Padding(padding: const EdgeInsets.all(12.0), child: child),
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});

  @override
  Widget build(BuildContext context) {
    return const Center(child: CircularProgressIndicator());
  }
}

class AppDialog {
  static Future<void> showInfo(
      BuildContext context, String title, String message) async {
    return showDialog(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(c).pop(), child: const Text('OK')),
        ],
      ),
    );
  }
}
