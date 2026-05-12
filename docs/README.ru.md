
---

## Файл 2: `docs/README.ru.md` (Русская версия)

```markdown
<p align="center">
  <img src="../app/src/main/res/drawable/abstract_logo.png" width="128" alt="Abstract IDE Logo">
</p>

<h1 align="center">Abstract IDE</h1>

<p align="center">
  <b>Визуальная среда программирования, где код собирается как конструктор.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" alt="Java">
  <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## 🔥 Почему Abstract IDE?

Представь себе **Scratch**, но с безграничными возможностями. Ты можешь не просто учиться программировать, а создавать полноценные программы с поддержкой плагинов, вложенными контейнерами и умной системой связей между блоками.

Вот что превращает этот проект из простого "конструктора" в инструмент для настоящего разработчика:

### 🤖 Технические фишки для профессионалов

*   **Графовая архитектура**: Все блоки хранятся как граф с направленными связями (`FROM port -> TO port`), а последовательность выполнения вычисляется через топологическую сортировку (алгоритм DFS). Ты видишь не просто список, а логику программы.
*   **Умные контейнеры**: Блоки сами знают, кто их "родитель" и "дети". Контейнеры (`if/else`, `for`) динамически меняют свой размер, чтобы вместить вложенные блоки. **Nesting** (вложение) происходит с анимацией и защитой от циклических связей.
*   **Система Call-блоков**: Внутри контейнеров (как списки или словари) создаются специальные `call_` блоки. Они хранят ссылку на оригинальный блок, что позволяет вызывать переменные и функции без дублирования кода.
*   **Сериализация в JSON**: Проект сохраняется в единый `.abstract` файл. Это JSON с полным графом, который легко читать, изменять вручную или использовать для систем контроля версий (Git).

### ⚙️ Архитектура (Граф)

Эта схема показывает, как блоки связаны друг с другом и как они вложены. Это и есть сердце программы.

```mermaid
graph TD
    A[Variable x = 5] --> B[Print]
    C[List Container] -...-> D[call_Variable]
    A -...-> C
    
    E[If Block]
    E1[Condition Container]
    E --- E1
    
    E1 --> G[Operator ==]
    E1 --> H[Value 10]
    E1 --> I[Variable x]
    
    E --> J[True Branch]
    E --> K[False Branch]
    J -...-> L[Print Yes]
    K -...-> M[Print No]