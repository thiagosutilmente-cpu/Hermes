with open('index.html', 'r') as f:
    content = f.read()

content = content.replace("dica do Jarvis: caso a entrega seja em apartamento ou condomínio, diga 'Avisa o cliente' e eu preparo uma mensagem para ele já ir descendo.\",", "dica do Jarvis: caso a entrega seja em apartamento ou condomínio, diga 'Avisa o cliente' e eu preparo uma mensagem para ele já ir descendo.\`,")

with open('index.html', 'w') as f:
    f.write(content)
