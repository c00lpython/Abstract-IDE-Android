<p align="center">
  <img src="../app/src/main/res/drawable/abstract_logo.png" width="128" alt="Abstract IDE Logo">
</p>

<h1 align="center">Abstract IDE</h1>

<p align="center">
  <b>A visual programming environment where you build code like Lego bricks.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" alt="Java">
  <img src="https://img.shields.io/badge/Status-Active-success?style=for-the-badge" alt="Status">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License">
</p>

---

## 🔥 Why Abstract IDE?

Think of **Scratch**, but with unlimited possibilities. You're not just learning to code — you're building real programs with plugin support, nested containers, and a smart block connection system.

Here's what turns this project from a simple "builder" into a tool for actual developers:

### 🤖 Technical Features for Pros

*   **Graph-Based Architecture**: All blocks are stored as a directed graph (`FROM port -> TO port`). Execution order is calculated via topological sorting (DFS algorithm). You see not just a list, but the actual logic of your program.
*   **Smart Containers**: Blocks know their parent and children. Containers (`if/else`, `for`) dynamically resize to fit nested blocks. **Nesting** happens with smooth animations and protection against circular references.
*   **Call-Block System**: Inside containers (like lists or dictionaries), special `call_` blocks are created. They store a reference to the original block, allowing you to call variables and functions without duplicating code.
*   **JSON Serialization**: Projects are saved as a single `.abstract` file — a JSON blob containing the full graph. Easy to read, edit manually, or track with version control systems like Git.

### ⚙️ Architecture (Graph)

This diagram shows how blocks connect to each other and how nesting works. It's the heart of the application.

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