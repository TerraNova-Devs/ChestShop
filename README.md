# ChestShop

ChestShop is a Minecraft plugin that allows players to create and manage shops using chests and signs. Players can buy and sell items through these shops, providing an in-game economy system.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Methods](#methods)

## Features

- Create shops using chests and signs
- Buy and sell items
- Supports non-stackable items
- Configurable shop settings
- Adventure API for modern Minecraft versions
- Persistent data storage

## Installation

1. **Download the Plugin**
   - Download the latest version of the ChestShop plugin from the [Releases](https://github.com/Pixel-Party-Devs/ChestShop/releases) page.

2. **Install the Plugin**
   - Place the downloaded JAR file into your server's `plugins` directory.

3. **Start the Server**
   - Start your Minecraft server to generate the default configuration files.

## Usage

### Creating a Shop

1. **Place a Chest**
   - Place a chest where you want to create the shop.

2. **Place a Sign**
   - Place a sign on the chest or adjacent to it.
   - Format the sign as follows:
     ```
     [Shop]
     B:<Buy Price>
     S:<Sell Price>
     <Quantity>
     ```
     *The sign must contain at least 1 of the prices specified. If no selling is wished, the 3rd line could be left empty.*

3. **Configure the Shop**
   - The plugin will automatically detect the shopItem depending on which Item is in the first chest slot at creation.
   - The plugin will automatically detect the sign and create the shop.

### Buying and Selling Items

- **Buy Items**
  - Right-click the sign to buy the item.
- **Sell Items**
  - Left-click the sign to sell the item.

## Methods

### Listeners

#### ShopListener

Handles the main functionality of creating, interacting with, and managing shops.

#### ShopInventoryListener

Manages the interaction with shop inventories, ensuring items are correctly handled when players interact with the shop interface.

#### ShopCreationListener

Handles the creation of shops, ensuring signs and chests are correctly configured when a new shop is created.

#### ShopDeletionListener

Manages the deletion of shops, ensuring the appropriate cleanup of data and resources when a shop is removed.

#### ChestAccessListener

Controls access to chests linked to shops, preventing unauthorized players from accessing shop inventories.

#### ChestProtectionListener

Protects chests from being tampered with through various methods, such as blocking, breaking, or placing signs.

### Utility Methods

- `InventoryHelper`: Utility methods for managing inventories.
- `ChatUtils`: Utility methods for sending chat messages to players.
- `SilverManager`: Manages the in-game currency.
