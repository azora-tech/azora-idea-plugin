<p align="center">
  <img src="src/main/resources/icons/azora_logo.png" alt="Azora Logo" width="128" />
</p>

<h1 align="center">Azora Language Plugin for JetBrains IDEs</h1>

<p align="center">
  Official IDE plugin bringing full <a href="https://azoralang.org">Azora</a> language support to
  <a href="https://www.jetbrains.com/idea/">IntelliJ IDEA</a> and
  <a href="https://developer.android.com/studio">Android Studio</a>.
</p>

<p align="center">
  <a href="https://azoralang.org">Website</a> &middot;
  <a href="https://docs.azoralang.org">Docs</a> &middot;
  <a href="https://book.azoralang.org">Book</a> &middot;
  <a href="https://code.azoralang.org">Playground</a>
</p>

---

## Features

- **Syntax highlighting** with semantic keyword coloring (declaration, control-flow, modifier, memory, reactive)
- **Code completion** for keywords, symbols, dot access, scope access, pack constructors, enum/slot/fail variants, and code snippets
- **Go-to-definition** (Ctrl+Click / Cmd+Click) for identifiers across files
- **Quick documentation** (Ctrl+Q / F1) showing type signatures, parameters, fields, and variants
- **Error detection** for unmatched braces and unterminated strings
- **Brace matching** and **code folding** for declarations, comments, imports, and regions
- **Structure view** with outline of functions, packs, enums, views, impls, tests, and more
- **Find usages** for identifiers across the project
- **Line and block commenting** (`//` and block comments)
- **Run configurations** with interpret and native targets, plus a gutter run icon on `func main()`
- **New Project wizard** with SDK path configuration and `azora.toml` scaffolding
- **Build sync** from `azora.toml` with editor notification banners
- **Color scheme settings** page for full customization of Azora syntax colors

## Requirements

- [IntelliJ IDEA](https://www.jetbrains.com/idea/) 2025.3+ or [Android Studio](https://developer.android.com/studio) Meerkat+
- [Azora SDK](https://azoralang.org) installed (for run configurations)

## Installation

### From source

```bash
git clone https://github.com/azora-tech/azora-idea-plugin.git
cd azora-idea-plugin
./gradlew build
```

The built plugin zip will be in `build/distributions/`. Install it via **Settings > Plugins > Install Plugin from Disk**.

## Getting Started

1. Install the plugin
2. **File > New > Azora Project** (or use the New Project wizard)
3. Set your Azora SDK path (e.g. `~/.azoralang`)
4. Start coding in `.az` files

The plugin will automatically create a `src/Main.az` entry point and an `azora.toml` project manifest. Run configurations for each target defined in `azora.toml` are created automatically.

## Project Structure

```
my-project/
  azora.toml          # Project manifest (name, version, targets, entry)
  src/
    Main.az            # Entry point
    ...
```

## Links

| Resource | URL |
|----------|-----|
| Azora Website | [azoralang.org](https://azoralang.org) |
| Documentation | [docs.azoralang.org](https://docs.azoralang.org) |
| The Azora Book | [book.azoralang.org](https://book.azoralang.org) |
| Source Code | [code.azoralang.org](https://code.azoralang.org) |
| IntelliJ IDEA | [jetbrains.com/idea](https://www.jetbrains.com/idea/) |
| Android Studio | [developer.android.com/studio](https://developer.android.com/studio) |

## License

Licensed under the [Apache License 2.0](LICENSE).

Copyright 2026 AzoraTech.
