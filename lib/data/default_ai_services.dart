import 'package:ai_hub/models/ai_service.dart';

class DefaultAIServices {
  static final List<AIService> services = [
    AIService(
      id: 'chatgpt',
      name: 'ChatGPT',
      url: 'https://chatgpt.com',
      faviconUrl: 'https://openai.com/favicon.ico',
      iconPath:
          'https://cdn.freebiesupply.com/logos/large/2x/chatgpt-symbol.png',
      createdAt: DateTime.now(),
    ),
    AIService(
      id: 'claude',
      name: 'Claude',
      url: 'https://claude.ai',
      faviconUrl: 'https://claude.ai/favicon.ico',
      iconPath:
          'https://freepnglogo.com/images/all_img/claude-ai-icon-65aa.png',
      createdAt: DateTime.now(),
    ),
    AIService(
      id: 'deepseek',
      name: 'DeepSeek',
      url: 'https://chat.deepseek.com',
      faviconUrl: 'https://www.deepseek.com/favicon.ico',
      iconPath: 'https://www.deepseek.com/favicon.ico',
      createdAt: DateTime.now(),
    ),
    AIService(
      id: 'grok',
      name: 'Grok',
      url: 'https://grok.com',
      faviconUrl: 'https://grok.com/favicon.ico',
      iconPath:
          'https://cdn.freelogovectors.net/wp-content/uploads/2025/06/grok_logo-freelogovectors.net_.png',
      createdAt: DateTime.now(),
    ),
    AIService(
      id: 'gemini',
      name: 'Gemini',
      url: 'https://gemini.google.com',
      faviconUrl:
          'https://www.gstatic.com/lamda/images/favicon_v1_150160d13f3925574452.png',
      iconPath:
          'https://cdn.freebiesupply.com/logos/large/2x/google-gemini-icon.png',
      createdAt: DateTime.now(),
    ),
    AIService(
      id: 'perplexity',
      name: 'Perplexity',
      url: 'https://www.perplexity.ai',
      faviconUrl: 'https://www.perplexity.ai/favicon.ico',
      iconPath:
          'https://images.seeklogo.com/logo-png/55/3/perplexity-logo-png_seeklogo-554497.png',
      createdAt: DateTime.now(),
    ),
  ];
}
