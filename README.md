# SunTNT
A modern and modular plugin for custom TNT types in Minecraft 1.16.5, built with the [SunCore](https://github.com/7byloper/SunCore) library.

## ✨ Features
- Fully customizable TNTs via configuration
- Create new types of TNT with unique effects
- Easy-to-use command system: `/tnt give`, `/tnt customitems`, etc.
- Listener-based architecture with clean separation of logic
- Lightweight and optimized for Paper 1.16.5

## 📁 TNT Types Included
- **TNTA** – Basic explosion
- **TNTICE** – Freezes surroundings
- **TNTSPAWNER** – Spawns entities on detonation
- **TNTAQUA** – Water-based effects

## ⚙️ Configuration
All TNT types are configurable in `/plugins/SunTNT/tnts/`.  
You can also configure:
- Plugin behavior (`config.yml`)
- Custom item appearances (`customItems.yml`)

## 🧱 Commands
- `/tnt give <player> <type>` – Give a specific TNT
- `/tnt customitems` – Reload or list custom items

## 📦 Dependencies
- Requires [SunCore](https://github.com/7byloper/SunCore)
- Built with Java 8+ and Maven

---

*Developed with ❤️ by ImLoper*
