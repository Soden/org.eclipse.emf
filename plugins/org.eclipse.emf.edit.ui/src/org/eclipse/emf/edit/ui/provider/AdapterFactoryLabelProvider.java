/**
 * <copyright> 
 *
 * Copyright (c) 2002-2006 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *   IBM - Initial API and implementation
 *
 * </copyright>
 *
 * $Id: AdapterFactoryLabelProvider.java,v 1.4 2006/12/28 06:50:05 marcelop Exp $
 */
package org.eclipse.emf.edit.ui.provider;


import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.edit.EMFEditPlugin;
import org.eclipse.emf.edit.provider.IChangeNotifier;
import org.eclipse.emf.edit.provider.IItemLabelProvider;
import org.eclipse.emf.edit.provider.INotifyChangedListener;
import org.eclipse.emf.edit.provider.ITableItemLabelProvider;


/**
 * This label provider wraps an AdapterFactory
 * and it delegates its JFace provider interfaces to corresponding adapter-implemented item provider interfaces.
 * All method calls to the various label provider interfaces
 * are delegated to interfaces implemented by the adapters generated by the AdapterFactory.
 * {@link ILabelProvider} is delegated to {@link IItemLabelProvider};
 * and {@link ITableLabelProvider} is delegated to {@link ITableItemLabelProvider}.
 * <p>
 * The label provider has no mechanism for notifying the viewer of changes.
 * As long as the AdapterFactory is also used in an AdapterFactoryContentProvider, this won't be a problem,
 * because notifications will be forward as a result of that.
 */
public class AdapterFactoryLabelProvider implements ILabelProvider, ITableLabelProvider, INotifyChangedListener
{
  /**
   * This keep track of the one factory we are using.
   * Use a {@link org.eclipse.emf.edit.provider.ComposedAdapterFactory} 
   * if adapters from more the one factory are involved in the model.
   */
  protected AdapterFactory adapterFactory;

  /**
   * This keeps track of the label provider listeners.
   */
  protected Collection<ILabelProviderListener> labelProviderListeners;

  private static final Class<?> IItemLabelProviderClass = IItemLabelProvider.class;
  private static final Class<?> ITableItemLabelProviderClass = ITableItemLabelProvider.class;

  /**
   *  Construct an instance that wraps this factory.
   *  The AdapterFactory should yield adapters that implement the various item label provider interfaces.
   */
  public AdapterFactoryLabelProvider(AdapterFactory adapterFactory)
  {
    this.adapterFactory = adapterFactory;
    if (adapterFactory instanceof IChangeNotifier)
    {
      ((IChangeNotifier)adapterFactory).addListener(this);
    }

    labelProviderListeners = new ArrayList<ILabelProviderListener>();
  }

  /**
   * Return the wrapped AdapterFactory.
   */
  public AdapterFactory getAdapterFactory()
  {
    return adapterFactory;
  }

  /**
   * Set the wrapped AdapterFactory.
   */
  public void setAdapterFactory(AdapterFactory adapterFactory)
  {
    if (this.adapterFactory instanceof IChangeNotifier)
    {
      ((IChangeNotifier)this.adapterFactory).removeListener(this);
    }

    if (adapterFactory instanceof IChangeNotifier)
    {
      ((IChangeNotifier)adapterFactory).addListener(this);
    }

    this.adapterFactory = adapterFactory;
  }

  /**
   * Since we won't ever generate these notifications, we can just ignore this.
   */
  public void addListener(ILabelProviderListener listener) 
  {
    labelProviderListeners.add(listener);
  }

  /**
   * Since we won't ever add listeners, we can just ignore this.
   */
  public void removeListener(ILabelProviderListener listener)
  {
    labelProviderListeners.remove(listener);
  }

  /**
   * This discards the content provider and removes this as a listener to the {@link #adapterFactory}.
   */
  public void dispose()
  {
    if (this.adapterFactory instanceof IChangeNotifier)
    {
      ((IChangeNotifier)adapterFactory).removeListener(this);
    }
  }

  /**
   * This always returns true right now.
   */
  public boolean isLabelProperty(Object object, String id)
  {
    return true;
  }

  /**
   * This implements {@link org.eclipse.jface.viewers.ILabelProvider}.getImage by forwarding it to an object that implements 
   * {@link org.eclipse.emf.edit.provider.IItemLabelProvider#getImage IItemLabelProvider.getImage}
   */
  public Image getImage(Object object) 
  {
    // Get the adapter from the factory.
    //
    IItemLabelProvider itemLabelProvider = (IItemLabelProvider)adapterFactory.adapt(object, IItemLabelProviderClass);

    return 
      itemLabelProvider != null ?
        getImageFromObject(itemLabelProvider.getImage(object)) :
        getDefaultImage(object);
  }

  protected Image getDefaultImage(Object object)
  {
    String image = "full/obj16/GenericValue";
    if (object instanceof String)
    {
      image = "full/obj16/TextValue";
    }
    else if (object instanceof Boolean)
    {
      image = "full/obj16/BooleanValue";
    }
    else if (object instanceof Float || object instanceof Double)
    {
      image = "full/obj16/RealValue";
    }
    else if (object instanceof Integer || object instanceof Short || object instanceof Long || object instanceof Byte)
    {
      image = "full/obj16/RealValue";
    }

    return getImageFromObject(EMFEditPlugin.INSTANCE.getImage(image));
  }

  protected Image getImageFromObject(Object object)
  {
    return ExtendedImageRegistry.getInstance().getImage(object);
  }

  /**
   * This implements {@link ILabelProvider}.getText by forwarding it to an object that implements 
   * {@link IItemLabelProvider#getText IItemLabelProvider.getText}
   */
  public String getText(Object object) 
  {
    // Get the adapter from the factory.
    //
    IItemLabelProvider itemLabelProvider = (IItemLabelProvider)adapterFactory.adapt(object, IItemLabelProviderClass);

    return
      itemLabelProvider != null ?
        itemLabelProvider.getText(object) :
        object == null ? 
          "" :
          object.toString();
  }

  /**
   * This implements {@link ITableLabelProvider}.getColumnmage by forwarding it to an object that implements 
   * {@link ITableItemLabelProvider#getColumnImage ITableItemLabelProvider.getColumnImage}
   * or failing that, an object that implements 
   * {@link IItemLabelProvider#getImage IItemLabelProvider.getImage}
   * where the columnIndex is ignored.
   */
  public Image getColumnImage(Object object, int columnIndex)
  {
    // Get the adapter from the factory.
    //
    ITableItemLabelProvider tableItemLabelProvider = (ITableItemLabelProvider)adapterFactory.adapt(object, ITableItemLabelProviderClass);

    // No image is a good default.
    //
    Image result = null;

    // Now we could check that the adapter implements interface ITableItemLabelProvider.
    //
    if (tableItemLabelProvider  != null)
    {
      // And delegate the call.
      //
      result = getImageFromObject(tableItemLabelProvider.getColumnImage(object, columnIndex));
    }
    // Otherwise, we could check that the adapter implements interface IItemLabelProvider.
    //
    else 
    {
      IItemLabelProvider itemLabelProvider = (IItemLabelProvider)adapterFactory.adapt(object, IItemLabelProviderClass);
      if (itemLabelProvider != null)
      {
        // And delegate the call.
        //
        result = getImageFromObject(itemLabelProvider.getImage(object));
      }
    }

    return result;
  }

  /**
   * This implements {@link ITableLabelProvider}.getColumnText by forwarding it to an object that implements 
   * {@link ITableItemLabelProvider#getColumnText ITableItemLabelProvider.getColumnText}
   * or failing that, an object that implements 
   * {@link IItemLabelProvider#getText IItemLabelProvider.getText}
   * where the columnIndex are is ignored.
   */
  public String getColumnText(Object object, int columnIndex)
  {
    // Get the adapter from the factory.
    //
    ITableItemLabelProvider tableItemLabelProvider = (ITableItemLabelProvider)adapterFactory.adapt(object, ITableItemLabelProviderClass);

    // Now we could check that the adapter implements interface ITableItemLabelProvider.
    //
    if (tableItemLabelProvider != null)
    {
      // And delegate the call.
      //
      return tableItemLabelProvider.getColumnText(object, columnIndex);
    }
    // Otherwise, we could check that the adapter implements interface IItemLabelProvider.
    //
    else 
    {
      IItemLabelProvider itemLabelProvider = (IItemLabelProvider)adapterFactory.adapt(object, IItemLabelProviderClass);
      if (itemLabelProvider != null)
      {
        // And delegate the call.
        //
        return itemLabelProvider.getText(object);
      }
      // If there is a column object, just convert it to a string.
      //
      else if (object != null)
      {
        return object.toString();
      }
      else
      {
      return "";
      }
    }
  }

  public void fireLabelProviderChanged()
  {
    for (ILabelProviderListener labelProviderListener : labelProviderListeners)
    {
      labelProviderListener.labelProviderChanged(new LabelProviderChangedEvent(this));
    }
  }

  public void notifyChanged(Notification notification)
  {
    // fireLabelProviderChanged();
  }
}
