package jenkins.plugins.glip.webhook.GlobalConfig;

f = namespace('/lib/form')

f.section(title: _('Global Glip Notifier Settings')) {
    f.entry(field: 'webhookUrl', title: _('Webhook URL')) {
        f.textbox()
    }
}
