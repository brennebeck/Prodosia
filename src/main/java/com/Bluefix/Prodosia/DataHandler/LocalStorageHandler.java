/*
 * Copyright (c) 2018 J.S. Boellaard
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.Bluefix.Prodosia.DataHandler;

import com.Bluefix.Prodosia.SQLite.SqlBuilder;
import com.Bluefix.Prodosia.SQLite.SqlDatabase;

import java.sql.PreparedStatement;
import java.util.ArrayList;

/**
 * A base class that allows for a Handler to have a local storage.
 */
public abstract class LocalStorageHandler <T>
{
    //region Variables and constructor

    /**
     * The local storage data
     */
    private ArrayList<T> data;

    public LocalStorageHandler()
    {
        data = null;
        this.useLocalStorage = true;
    }

    /**
     * Initialize a Local Storage Handler, indicating whether the local storage should be used.
     * @param useLocalStorage boolean indication whether local storage should be used.
     */
    public LocalStorageHandler(boolean useLocalStorage)
    {
        data = null;
        this.useLocalStorage = useLocalStorage;
    }

    //endregion

    //region use local storage

    private boolean useLocalStorage;

    /**
     * Indicate whether the Handler class should use local storage for data speedup.
     * @param val The boolean value. True means local storage is used for speedup, at the cost of memory.
     */
    public void setLocalStorage(boolean val)
    {
        this.useLocalStorage = val;

        // delete the local storage if it was disabled.
        if (!this.useLocalStorage)
            data = null;
    }

    //endregion




    //region Data handling


    public void set(T t) throws Exception
    {
        // if t did not exist, skip this phase.
        if (t == null)
            return;

        // set the item and retrieve the old item.
        T oldItem = setItem(t);

        // if the local storage is used, replace the item.
        if (useLocalStorage)
        {
            // initialize the local storage if necessary.
            if (data == null)
            {
                data = getAllItems();
            }
            else
            {
                // update
                data.remove(oldItem);
                data.add(t);
            }
        }
    }


    /**
     * Remove an item from the collection.
     * @param t The item to be removed.
     */
    public void remove(T t) throws Exception
    {
        // if t did not exist, skip this phase.
        if (t == null)
            return;

        // if the local storage is in use, remove the item
        if (useLocalStorage && data != null)
            data.remove(t);

        // remove the item from the database.
        removeItem(t);
    }


    /**
     * Update the old item, replacing it with the new one.
     * @param oldT The old item to be replaced.
     * @param newT The new replacement item.
     * @throws Exception
     */
    public void update(T oldT, T newT) throws Exception
    {
        remove(oldT);
        set(newT);
    }




    /**
     * Retrieve all items from the list.
     * @return An arraylist with all items currently stored by the handler.
     */
    public ArrayList<T> getAll() throws Exception
    {
        // if the local storage is used,
        if (useLocalStorage)
        {
            if (data == null)
                data = getAllItems();

            return data;
        }

        // if the local storage wasn't used, simply return the data call.
        return getAllItems();
    }


    //endregion


    //region Abstract methods

    /**
     * Remove an item from the storage.
     * @param t
     */
    protected abstract void removeItem(T t) throws Exception;

    /**
     * Set the item, replacing the existing item if applicable.
     * @param t
     * @return The old item that was replaced, or null if no item had to be replaced.
     * @throws Exception
     */
    protected abstract T setItem(T t) throws Exception;

    /**
     * Retrieve all items from the storage in no particular order.
     * @return
     */
    protected abstract ArrayList<T> getAllItems() throws Exception;

    //endregion
}
