# Inventário de textos do protótipo

Este documento cobre somente as seis telas do épico de linguagem: Entrar, Cadastrar, Meus vídeos, Detalhe do vídeo, Enviar vídeo e Meu perfil, além do shell compartilhado. Os textos vêm de `src/product-copy.ts`. Arquivos, e-mail e datas são dados, não textos literais.

## Variáveis de padrões dinâmicos

| Variável | Origem | Formatação |
| --- | --- | --- |
| `{data}` | `activityAt` ISO UTC | `Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })` no fuso do navegador |
| `{nome}` | `File.name` selecionado | texto simples |
| `{tamanho}` | `File.size` | KB/MB em `pt-BR` |
| `{percent}` | progresso do mock, 0 a 100 | número inteiro |

## Correspondência de estados técnicos → produto

A lista e o detalhe mostram um único status de ciclo de vida vindo do contrato de biblioteca. Enums internos não aparecem na interface.

| Status no contrato | Texto visível |
| --- | --- |
| `AWAITING_UPLOAD` | Pendente |
| `UPLOADED` | Processando |
| `PROCESSING` | Processando |
| `AVAILABLE` | Processado |
| `REJECTED` | Rejeitado |
| `EXPIRED` | Expirado |
| `FAILED` | Falha no processamento |

Marcos futuros mostram `Aguardando`. Dado histórico ausente mostra `Data indisponível`.

## Inventário

| Chave | Tela | Texto ou padrão | Condição | Tipo |
| --- | --- | --- | --- | --- |
| access.kicker | Entrar | Acesso | painel de acesso | fixo |
| access.heroTitle | Entrar | Extraia imagens dos seus vídeos. | painel ilustrativo | fixo |
| access.heroLead | Entrar | Envie um vídeo, acompanhe o processamento e baixe as imagens quando estiverem prontas. | painel ilustrativo | fixo |
| access.stepSend | Entrar | Envie o vídeo | etapas ilustrativas | fixo |
| access.stepProcess | Entrar | Extraia as imagens | etapas ilustrativas | fixo |
| access.stepDownload | Entrar | Baixe o pacote | etapas ilustrativas | fixo |
| access.enterTab | Entrar | Entrar | aba | fixo |
| access.createTab | Cadastrar | Criar conta | aba | fixo |
| access.loginTitle | Entrar | Entre para acompanhar seus vídeos. | formulário de entrada | fixo |
| access.loginLead | Entrar | Use seu e-mail e senha para acessar a FIAP X. | formulário de entrada | fixo |
| access.registerTitle | Cadastrar | Crie sua conta. | formulário de cadastro | fixo |

| access.emailLabel | Entrar | E-mail | campo | fixo |
| access.passwordLabel | Entrar | Senha | campo | fixo |
| access.confirmPasswordLabel | Cadastrar | Confirmar senha | campo | fixo |
| access.loginSubmit | Entrar | Entrar | ação primária | fixo |
| access.loginPending | Entrar | Entrando… | envio em andamento | fixo |
| access.registerSubmit | Cadastrar | Criar conta | ação primária | fixo |
| access.registerPending | Cadastrar | Criando conta… | envio em andamento | fixo |
| access.emailRequired | Entrar | Informe seu e-mail. | e-mail vazio | fixo |
| access.emailInvalid | Entrar | Informe um e-mail válido. | e-mail malformado | fixo |
| access.passwordRequired | Entrar | Informe sua senha. | senha vazia | fixo |
| access.passwordLength | Entrar | A senha deve ter entre 8 e 128 caracteres. | senha fora do intervalo | fixo |
| access.confirmRequired | Cadastrar | Confirme sua senha. | confirmação vazia | fixo |
| access.passwordMismatch | Cadastrar | As senhas não coincidem. | senhas diferentes | fixo |
| access.invalidCredentials | Entrar | E-mail ou senha inválidos. | credenciais rejeitadas | fixo |
| access.loginUnavailable | Entrar | Não foi possível entrar. Tente novamente. | falha inesperada | fixo |
| access.registerDuplicate | Cadastrar | Este e-mail já está cadastrado. | e-mail duplicado | fixo |
| access.registerUnavailable | Cadastrar | Não foi possível criar a conta. Tente novamente. | falha inesperada | fixo |
| access.registerSuccess | Entrar | Conta criada. Entre com o e-mail e a senha cadastrados. | após cadastro válido | fixo |
| access.checkingSession | Entrar | Verificando seu acesso… | verificação inicial da sessão | fixo |
| access.sessionEnded | Entrar | Sua sessão terminou. Entre novamente. | sessão expirada ou inválida | fixo |
| access.sessionCheckUnavailable | Entrar | Não foi possível verificar seu acesso. Tente novamente. | falha temporária ao verificar acesso | fixo |
| access.retrySessionCheck | Entrar | Tentar novamente | falha temporária ao verificar acesso | fixo |

| shell.navVideos | Shell compartilhado | Meus vídeos | menu | fixo |
| shell.navUpload | Shell compartilhado | Enviar vídeo | menu | fixo |
| shell.navProfile | Shell compartilhado | Meu perfil | menu | fixo |
| shell.logout | Shell compartilhado | Sair | encerrar sessão | fixo |
| shell.logoutPending | Shell compartilhado | Saindo… | saída em andamento | fixo |
| shell.logoutUnavailable | Shell compartilhado | Não foi possível sair. Tente novamente. | falha temporária ao sair | fixo |
| shell.logoutRetry | Shell compartilhado | Tentar novamente | falha temporária ao sair | fixo |
| shell.openMenu | Shell compartilhado | Abrir menu | menu móvel fechado | fixo |
| shell.closeMenu | Shell compartilhado | Fechar menu | menu móvel aberto | fixo |

| status.* | Meus vídeos / Detalhe | ver tabela de estados | estado visível do vídeo | fixo |
| videos.title | Meus vídeos | Meus vídeos | cabeçalho | fixo |
| videos.lead | Meus vídeos | Acompanhe o envio e o processamento dos seus arquivos. | cabeçalho | fixo |
| videos.searchLabel | Meus vídeos | Buscar por nome do vídeo | campo de busca | fixo |
| videos.searchPlaceholder | Meus vídeos | Buscar por nome... | campo de busca vazio | fixo |
| videos.searchExactAction | Meus vídeos | Buscar nome completo | envio do campo de busca | fixo |
| videos.statusFilterLabel | Meus vídeos | Filtrar por status | grupo de filtros | fixo |
| videos.statusFilterButton | Meus vídeos | Status | filtro fechado sem seleção | fixo |
| videos.statusAll | Meus vídeos | Todos | filtro sem restrição | fixo |
| videos.statusProcessed | Meus vídeos | Processado | filtro de processados | fixo |
| videos.statusProcessing | Meus vídeos | Processando | filtro em processamento | fixo |
| videos.statusFailed | Meus vídeos | Falha no processamento | filtro com falha | fixo |
| videos.clearCriteria | Meus vídeos | Limpar busca e filtros | busca ou filtro informado | fixo |
| videos.filteredEmptyTitle | Meus vídeos | Nenhum vídeo encontrado | consulta sem resultados | fixo |
| videos.filteredEmptyBody | Meus vídeos | Ajuste a busca ou o status para encontrar outros vídeos. | consulta sem resultados | fixo |
| videos.emptyTitle | Meus vídeos | Nenhum vídeo ainda | lista vazia | fixo |
| videos.emptyBody | Meus vídeos | Você ainda não enviou vídeos. | lista vazia | fixo |
| videos.emptyAction | Meus vídeos | Enviar o primeiro vídeo | lista vazia | fixo |
| videos.loading | Meus vídeos | Carregando seus vídeos. | carregamento | fixo |
| videos.error | Meus vídeos | Não foi possível carregar seus vídeos. | falha recuperável | fixo |
| videos.retry | Meus vídeos | Tentar de novo | falha recuperável | fixo |
| videos.openDetail | Meus vídeos | Ver detalhes | item da lista | fixo |
| videos.downloadPreparing | Meus vídeos | Resultado em preparação | indicador acessível para resultado ainda em processamento | fixo |
| videos.sortByColumn | Meus vídeos | Ordenar por {column} | cabeçalho ordenável inativo | dinâmico |
| videos.sortBy | Meus vídeos | Ordenar por {column}; ordem atual {direction} | cabeçalho ordenável | dinâmico |
| videos.sentAt | Meus vídeos | Enviado em {data} | item com horário de envio | dinâmico |
| videos.pagination | Meus vídeos | Paginação da biblioteca | navegação entre páginas | fixo |
| detail.back | Detalhe do vídeo | Voltar para meus vídeos | botão abaixo do detalhe | fixo |
| detail.timeline | Detalhe do vídeo | Andamento | linha do tempo | fixo |
| detail.sent | Detalhe do vídeo | Enviado em | marco | fixo |
| detail.processed | Detalhe do vídeo | Processado em | marco | fixo |
| detail.available | Detalhe do vídeo | Disponível em | marco | fixo |
| detail.awaiting | Detalhe do vídeo | Aguardando | marco futuro | fixo |
| detail.unavailable | Detalhe do vídeo | Data indisponível | dado histórico ausente | fixo |

| detail.notFound | Detalhe do vídeo | Não foi possível encontrar este vídeo. | vídeo inexistente para o dono | fixo |
| upload.title | Enviar vídeo | Enviar vídeo | cabeçalho | fixo |
| upload.lead | Enviar vídeo | Selecione um arquivo para simular o envio. Formatos aceitos: MP4, MOV, WebM e MKV. Limite de 500 MB (500.000.000 bytes). | instruções | fixo |
| upload.selected | Enviar vídeo | {nome} · {tamanho} | arquivo válido | dinâmico |
| upload.progress | Enviar vídeo | Enviando o vídeo… {percent}% | progresso | dinâmico |
| upload.success | Enviar vídeo | Vídeo enviado. Você já pode acompanhar o andamento em Meus vídeos. | sucesso simulado | fixo |
| upload.error | Enviar vídeo | Não foi possível enviar o vídeo. Tente novamente. | falha simulada | fixo |
| profile.title | Meu perfil | Meu perfil | cabeçalho | fixo |
| profile.lead | Meu perfil | Dados da conta em uso. | cabeçalho | fixo |
| profile.email | Meu perfil | E-mail | dado da sessão | fixo |
| profile.note | Meu perfil | A edição de perfil não está disponível nesta versão. | somente leitura | fixo |
| prototype.* | Shell compartilhado | Cenários de demonstração e opções | revisão de estados | fixo |

A lista completa e canônica das chaves está em `COPY_INVENTORY` no código.
