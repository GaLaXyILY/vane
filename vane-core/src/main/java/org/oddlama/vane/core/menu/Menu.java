package org.oddlama.vane.core.menu;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import org.oddlama.vane.core.functional.Consumer1;
import org.oddlama.vane.core.module.Context;

public class Menu {
	protected final MenuManager manager;
	protected Inventory inventory = null;
	private final Set<MenuWidget> widgets = new HashSet<>();
	private Consumer1<Player> on_close = null;

	protected Menu(final Context<?> context) {
		this.manager = context.get_module().core.menu_manager;
	}

	public Menu(final Context<?> context, final Inventory inventory) {
		this.manager = context.get_module().core.menu_manager;
		this.inventory = inventory;
	}

	public MenuManager manager() { return manager; }
	public Inventory inventory() { return inventory; }

	public void add(final MenuWidget widget) {
		widgets.add(widget);
	}

	public boolean remove(final MenuWidget widget) {
		return widgets.remove(widget);
	}

	public void update() { update(false); }
	public void update(boolean force_update) {
		int updated = widgets.stream()
			.mapToInt(w -> w.update(this) ? 1 : 0)
			.sum();

		if (updated > 0 || force_update) {
			// Send inventory content to players
			manager.update(this);
		}
	}

	public void open_window(final Player player) {
		player.openInventory(inventory);
	}

	public final void open(final Player player) {
		update(true);
		manager.add(player, this);
		manager.schedule_next_tick(() -> { open_window(player); });
	}

	public boolean close(final Player player) {
		return close(player, InventoryCloseEvent.Reason.PLUGIN);
	}

	public boolean close(final Player player, final InventoryCloseEvent.Reason reason) {
		final var top_inventory = player.getOpenInventory().getTopInventory();
		if (top_inventory != inventory) {
			try {
				throw new RuntimeException("Invalid close from unrelated menu.");
			} catch (RuntimeException e) {
				manager.get_module().log.log(Level.WARNING, "Tried to close menu inventory that isn't opened by the player " + player, e);
			}
			return false;
		}

		manager.schedule_next_tick(() -> { player.closeInventory(reason); });
		return true;
	}

	public Menu on_close(final Consumer1<Player> on_close) {
		this.on_close = on_close;
		return this;
	}

	public final void closed(final Player player) {
		if (on_close != null) {
			on_close.apply(player);
		}
		inventory.clear();
		manager.remove(player, this);
	}

	public ClickResult on_click(final Player player, final ItemStack item, int slot, final ClickType type, final InventoryAction action) {
		return ClickResult.IGNORE;
	}

	public final void click(final Player player, final ItemStack item, int slot, final ClickType type, final InventoryAction action) {
		// Ignore unknown click actions
		if (action == InventoryAction.UNKNOWN) {
			return;
		}

		// Send event to this menu
		var result = ClickResult.IGNORE;
		result = ClickResult.or(result, on_click(player, item, slot, type, action));

		// Send event to all widgets
		for (final var widget : widgets) {
			result = ClickResult.or(result, widget.click(player, this, item, slot, type, action));
		}

		switch (result) {
			default:
			case IGNORE: break;
			case SUCCESS:       player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK,       SoundCategory.MASTER, 1.0f, 1.0f); break;
			case ERROR:         player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0f, 1.0f); break;
			case INVALID_CLICK: player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.MASTER, 1.0f, 3.0f); break;
		}
	}

	public static boolean is_left_or_right_click(ClickType type, InventoryAction action) {
		return type == ClickType.LEFT || type == ClickType.RIGHT;
	}

	public static boolean is_left_click(ClickType type, InventoryAction action) {
		return type == ClickType.LEFT;
	}

	public static enum ClickResult {
		IGNORE(0),
		INVALID_CLICK(1),
		SUCCESS(2),
		ERROR(3);

		private int priority;
		private ClickResult(int priority) {
			this.priority = priority;
		}

		public static ClickResult or(final ClickResult a, final ClickResult b) {
			return a.priority > b.priority ? a : b;
		}
	}
}