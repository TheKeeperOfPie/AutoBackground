/*
 * Code by Stefan Rusek taken from Gist to fix possible Menu issue on some Android devices.
 * All credit goes to Mr. Rusek.
 *
 */

package cw.kop.autobackground;

import android.content.ComponentName;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

public abstract class MenuWrapper implements Menu {
    private final Menu menu;

    public MenuWrapper(Menu menu) {
        this.menu = menu;
    }

    @Override
    public MenuItem add(CharSequence title) {
        return menu.add(title);
    }

    @Override
    public MenuItem add(int titleRes) {
        return menu.add(titleRes);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        return menu.add(groupId, itemId, order, title);
    }

    @Override
    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return menu.add(groupId, itemId, order, titleRes);
    }

    @Override
    public SubMenu addSubMenu(CharSequence title) {
        return menu.addSubMenu(title);
    }

    @Override
    public SubMenu addSubMenu(int titleRes) {
        return menu.addSubMenu(titleRes);
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        return menu.addSubMenu(groupId, itemId, order, title);
    }

    @Override
    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        return menu.addSubMenu(groupId, itemId, order, titleRes);
    }

    @Override
    public int addIntentOptions(int groupId,
            int itemId,
            int order,
            ComponentName caller,
            Intent[] specifics,
            Intent intent,
            int flags,
            MenuItem[] outSpecificItems) {
        return menu.addIntentOptions(groupId,
                itemId,
                order,
                caller,
                specifics,
                intent,
                flags,
                outSpecificItems);
    }

    @Override
    public void removeItem(int id) {
        menu.removeItem(id);
    }

    @Override
    public void removeGroup(int groupId) {
        menu.removeGroup(groupId);
    }

    @Override
    public void clear() {
        menu.clear();
    }

    @Override
    public void setGroupCheckable(int group, boolean checkable, boolean exclusive) {
        menu.setGroupCheckable(group, checkable, exclusive);
    }

    @Override
    public void setGroupVisible(int group, boolean visible) {
        menu.setGroupVisible(group, visible);
    }

    @Override
    public void setGroupEnabled(int group, boolean enabled) {
        menu.setGroupEnabled(group, enabled);
    }

    @Override
    public boolean hasVisibleItems() {
        return menu.hasVisibleItems();
    }

    @Override
    public MenuItem findItem(int id) {
        return menu.findItem(id);
    }

    @Override
    public int size() {
        return menu.size();
    }

    @Override
    public MenuItem getItem(int index) {
        return menu.getItem(index);
    }

    @Override
    public void close() {
        menu.close();
    }

    @Override
    public boolean performShortcut(int keyCode, KeyEvent event, int flags) {
        return menu.performShortcut(keyCode, event, flags);
    }

    @Override
    public boolean isShortcutKey(int keyCode, KeyEvent event) {
        return menu.isShortcutKey(keyCode, event);
    }

    @Override
    public boolean performIdentifierAction(int id, int flags) {
        return menu.performIdentifierAction(id, flags);
    }

    @Override
    public void setQwertyMode(boolean isQwerty) {
        menu.setQwertyMode(isQwerty);
    }
}