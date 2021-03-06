/*
   Copyright (c) 2017 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import java.util.LinkedHashSet;
import java.util.List;

import com.ait.lienzo.client.core.Attribute;
import com.ait.lienzo.client.core.Context2D;
import com.ait.lienzo.client.core.config.LienzoCore;
import com.ait.lienzo.client.core.event.OnEventHandlers;
import com.ait.lienzo.client.core.event.OrientationChangeEvent;
import com.ait.lienzo.client.core.event.OrientationChangeHandler;
import com.ait.lienzo.client.core.event.ResizeChangeEvent;
import com.ait.lienzo.client.core.event.ResizeChangeHandler;
import com.ait.lienzo.client.core.event.ResizeEndEvent;
import com.ait.lienzo.client.core.event.ResizeEndHandler;
import com.ait.lienzo.client.core.event.ResizeStartEvent;
import com.ait.lienzo.client.core.event.ResizeStartHandler;
import com.ait.lienzo.client.core.event.OnMouseEventHandler;
import com.ait.lienzo.client.core.event.ViewportTransformChangedEvent;
import com.ait.lienzo.client.core.event.ViewportTransformChangedHandler;
import com.ait.lienzo.client.core.mediator.IMediator;
import com.ait.lienzo.client.core.mediator.Mediators;
import com.ait.lienzo.client.core.shape.json.validators.ValidationContext;
import com.ait.lienzo.client.core.shape.json.validators.ValidationException;
import com.ait.lienzo.client.core.shape.storage.IStorageEngine;
import com.ait.lienzo.client.core.shape.storage.ViewportFastArrayStorageEngine;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Transform;
import com.ait.lienzo.client.core.util.ScratchPad;
import com.ait.lienzo.shared.core.types.DataURLType;
import com.ait.lienzo.shared.core.types.NodeType;
import com.ait.tooling.common.api.java.util.function.Predicate;
import com.ait.tooling.nativetools.client.collection.MetaData;
import com.ait.tooling.nativetools.client.collection.NFastArrayList;
import com.google.gwt.dom.client.CanvasElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.Widget;

/**
 * Serves as a container for {@link Scene}
 * 
 * <ul>
 * <li>A {@link Viewport} contains three {@link Scene} (Main, Drag and Back Scene)</li>
 * <li>The main {@link Scene} can contain multiple {@link Layer}.</li>
 * </ul> 
 */
public class Viewport extends ContainerNode<Scene, Viewport>
{
    private int                    m_wide    = 0;

    private int                    m_high    = 0;

    private Widget                 m_owns    = null;

    private final DivElement       m_element = Document.get().createDivElement();

    private Scene                  m_drag    = new Scene();

    private Scene                  m_main    = null;

    private Scene                  m_back    = new Scene();

    private ScratchPad             m_spad    = new ScratchPad(0, 0);

    private Mediators              m_mediators;

    private final OnEventHandlers m_onEventHandlers = new OnEventHandlers();


    public Viewport()
    {
        this(0, 0);
    }

    public Viewport(final Scene main, final int wide, final int high)
    {
        super(NodeType.VIEWPORT, new ViewportFastArrayStorageEngine());

        m_wide = wide;

        m_high = high;

        setSceneAndState(main);
    }

    /**
     * Constructor. Creates an instance of a viewport.
     * 
     * @param wide
     * @param high
     */
    public Viewport(final int wide, final int high)
    {
        super(NodeType.VIEWPORT, new ViewportFastArrayStorageEngine());

        m_wide = wide;

        m_high = high;

        setSceneAndState(new Scene());
    }

    protected Viewport(final JSONObject node, final ValidationContext ctx) throws ValidationException
    {
        super(NodeType.VIEWPORT, node, ctx);
    }

    @Override
    public final IStorageEngine<Scene> getDefaultStorageEngine()
    {
        return new ViewportFastArrayStorageEngine();
    }

    public Transform getTransform()
    {
        return getAttributes().getTransform();
    }

    @Override
    protected Transform getPossibleNodeTransform()
    {
        return getTransform();
    }

    @Override
    public List<Attribute> getTransformingAttributes()
    {
        return LienzoCore.VIEWPORT_TRANSFORMING_ATTRIBUTES;
    }

    private final void setSceneAndState(final Scene main)
    {
        add(m_back, m_main = main, m_drag);

        m_drag.add(new DragLayer());

        m_drag.add(new Layer());

        m_mediators = new Mediators(this);

        final Transform transform = getTransform();

        if (null == transform)
        {
            // Zoom mediators rely on the Transform not being null.

            setTransform(new Transform());
        }
    }

    public final boolean adopt(final Widget owns)
    {
        if (null == m_owns)
        {
            m_owns = owns;

            return true;
        }
        return false;
    }

    @Override
    public final Viewport asViewport()
    {
        return this;
    }

    /**
     * Returns the viewport width in pixels.
     * 
     * @return int
     */
    public final int getWidth()
    {
        return m_wide;
    }

    /**
     * Returns the viewport height in pixels.
     * 
     * @return int
     */
    public final int getHeight()
    {
        return m_high;
    }

    /**
     * Returns the {@link DivElement}
     * 
     * @return {@link DivElement}
     */
    public final DivElement getElement()
    {
        return m_element;
    }

    /**
     * Sets size of the {@link Viewport} in pixels
     * 
     * @param wide
     * @param high
     * @return Viewpor this viewport
     */
    public final Viewport setPixelSize(final int wide, final int high)
    {
        m_wide = wide;

        m_high = high;

        getElement().getStyle().setWidth(wide, Unit.PX);

        getElement().getStyle().setHeight(high, Unit.PX);

        final NFastArrayList<Scene> scenes = getChildNodes();

        if (null != scenes)
        {
            final int size = scenes.size();

            for (int i = 0; i < size; i++)
            {
                final Scene scene = scenes.get(i);

                if (null != scene)
                {
                    scene.setPixelSize(wide, high);
                }
            }
        }
        m_spad.setPixelSize(wide, high);

        return this;
    }

    public OnEventHandlers getOnEventHandlers()
    {
        return m_onEventHandlers;
    }

    /**
     * Adds a {@link Scene} to this viewport.
     * 
     * @param scene
     */
    @Override
    public final Viewport add(final Scene scene)
    {
        if ((null != scene) && (LienzoCore.IS_CANVAS_SUPPORTED))
        {
            if (false == scene.adopt(this))
            {
                throw new IllegalArgumentException("Scene is already adopted.");
            }
            if (length() > 2)
            {
                throw new IllegalArgumentException("Too many Scene objects is Viewport.");
            }
            DivElement element = scene.getElement();

            scene.setPixelSize(m_wide, m_high);

            element.getStyle().setPosition(Position.ABSOLUTE);

            element.getStyle().setDisplay(Display.INLINE_BLOCK);

            getElement().appendChild(element);

            super.add(scene);
        }
        return this;
    }

    @Override
    public final Viewport add(final Scene scene, final Scene... children)
    {
        add(scene);

        for (Scene node : children)
        {
            add(node);
        }
        return this;
    }

    @Override
    public boolean removeFromParent()
    {
        return false;
    }

    public HandlerRegistration addOrientationChangeHandler(final OrientationChangeHandler handler)
    {
        return addEnsureHandler(OrientationChangeEvent.getType(), handler);
    }

    public HandlerRegistration addResizeStartHandler(final ResizeStartHandler handler)
    {
        return addEnsureHandler(ResizeStartEvent.getType(), handler);
    }

    public HandlerRegistration addResizeChangeHandler(final ResizeChangeHandler handler)
    {
        return addEnsureHandler(ResizeChangeEvent.getType(), handler);
    }

    public HandlerRegistration addResizeEndHandler(final ResizeEndHandler handler)
    {
        return addEnsureHandler(ResizeEndEvent.getType(), handler);
    }

    @Override
    public final Viewport draw()
    {
        final NFastArrayList<Scene> scenes = getChildNodes();

        if (null != scenes)
        {
            final int size = scenes.size();

            for (int i = 0; i < size; i++)
            {
                final Scene scene = scenes.get(i);

                if (null != scene)
                {
                    scene.draw();
                }
            }
        }
        return this;
    }

    @Override
    public final Viewport batch()
    {
        final NFastArrayList<Scene> scenes = getChildNodes();

        if (null != scenes)
        {
            final int size = scenes.size();

            for (int i = 0; i < size; i++)
            {
                final Scene scene = scenes.get(i);

                if (null != scene)
                {
                    scene.batch();
                }
            }
        }
        return this;
    }

    /**
     * Returns the main Scene for the {@link Viewport}
     * 
     * @return {@link Scene}
     */
    @Override
    public final Scene getScene()
    {
        return m_main;
    }

    /**
     * Sets the background layer
     * 
     * @param layer
     * @return this Viewport
     */
    public final Viewport setBackgroundLayer(final Layer layer)
    {
        m_back.removeAll();

        m_back.add(layer);

        return this;
    }

    /**
     * Returns the Drag Layer.
     * 
     * @return {@link Layer} 
     */
    public final Layer getDragLayer()
    {
        return m_drag.getChildNodes().get(0);
    }

    @Override
    public final Layer getOverLayer()
    {
        return m_drag.getChildNodes().get(1);
    }

    @Override
    public final Viewport getViewport()
    {
        return this;
    }

    @Override
    public final ScratchPad getScratchPad()
    {
        return m_spad;
    }

    /**
     * No-op; this method has no effect. Simply overriden but in reality Scenes will not be removed from this {@link Viewport}
     */
    @Override
    public final Viewport remove(final Scene scene)
    {
        return this;
    }

    /**
     * No-op; this method has no effect. Simply overriden but in reality Scenes will not be removed from this {@link Viewport}
     */
    @Override
    public final Viewport removeAll()
    {
        getScene().removeAll();

        return this;
    }

    /**
     * No-op.
     * 
     * @return this Viewport
     */
    @Override
    public final Viewport moveUp()
    {
        return this;
    }

    /**
     * No-op.
     * 
     * @return this Viewport
     */
    @Override
    public final Viewport moveDown()
    {
        return this;
    }

    /**
     * No-op.
     * 
     * @return this Viewport
     */
    @Override
    public final Viewport moveToTop()
    {
        return this;
    }

    /**
     * No-op.
     * 
     * @return this Viewport
     */
    @Override
    public final Viewport moveToBottom()
    {
        return this;
    }

    /**
     * Change the viewport's transform so that the specified area (in global or canvas coordinates)
     * is visible.
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public final void viewGlobalArea(double x, double y, double width, double height)
    {
        if (width <= 0 || height <= 0)
        {
            return;
        }
        final Transform t = getTransform();

        if (null != t)
        {
            Point2D a = new Point2D(x, y);

            Point2D b = new Point2D(x + width, y + height);

            final Transform inv = t.getInverse();

            inv.transform(a, a);

            inv.transform(b, b);

            x = a.getX();

            y = a.getY();

            width = b.getX() - x;

            height = b.getY() - y;
        }
        viewLocalArea(x, y, width, height);
    }

    /**
     * Change the viewport's transform so that the specified area (in local or world coordinates)
     * is visible.
     * 
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public final void viewLocalArea(final double x, final double y, final double width, final double height)
    {
        final Transform t = Transform.createViewportTransform(x, y, width, height, m_wide, m_high);

        if (t != null)
        {
            setTransform(t);
        }
    }

    /**
     * Sets the Transform for this Viewport and fires a ZoomEvent
     * to any ZoomHandlers registered with this Viewport.
     * 
     * 
     * @param transform Transform
     * @return this Viewport
     */

    public final Viewport setTransform(final Transform transform)
    {
        getAttributes().setTransform(transform);

        super.fireEvent(new ViewportTransformChangedEvent(this));

        return this;
    }

    /**
     * Returns a {@link JSONObject} representation of the {@link Viewport} with its {@link Attributes} as well as its children.
     * 
     * @return {@link JSONObject}
     */
    @Override
    public final JSONObject toJSONObject()
    {
        final JSONObject object = new JSONObject();

        object.put("type", new JSONString(getNodeType().getValue()));

        if (hasMetaData())
        {
            final MetaData meta = getMetaData();

            if (false == meta.isEmpty())
            {
                object.put("meta", new JSONObject(meta.getJSO()));
            }
        }
        object.put("attributes", new JSONObject(getAttributes().getJSO()));

        final JSONArray children = new JSONArray();

        children.set(0, getScene().toJSONObject());

        object.put("children", children);

        object.put("storage", getStorageEngine().toJSONObject());

        return object;
    }

    private final Layer getBackgroundLayer()
    {
        final NFastArrayList<Layer> list = m_back.getChildNodes();

        if (list.size() > 0)
        {
            return list.get(0);
        }
        return null;
    }

    public final Shape<?> findShapeAtPoint(final int x, final int y)
    {
        if (isVisible())
        {
            return getScene().findShapeAtPoint(x, y);
        }
        return null;
    }

    /**
     * Fires the given GWT event.
     */
    public final void fireEvent(final GwtEvent<?> event)
    {
        getScene().fireEvent(event);
    }

    public final String toDataURL()
    {
        return getScene().toDataURL();
    }

    public final String toDataURL(final boolean includeBackgroundLayer)
    {
        if (includeBackgroundLayer)
        {
            return getScene().toDataURL(getBackgroundLayer());
        }
        else
        {
            return getScene().toDataURL();
        }
    }

    public final String toDataURL(final DataURLType mimetype)
    {
        return getScene().toDataURL(mimetype);
    }

    public final String toDataURL(final DataURLType mimetype, final boolean includeBackgroundLayer)
    {
        if (includeBackgroundLayer)
        {
            return getScene().toDataURL(mimetype, getBackgroundLayer());
        }
        else
        {
            return getScene().toDataURL(mimetype);
        }
    }

    @Override
    protected void find(final Predicate<Node<?>> predicate, final LinkedHashSet<Node<?>> buff)
    {
        if (predicate.test(this))
        {
            buff.add(this);
        }
        m_main.find(predicate, buff);
    }

    /**
     * Returns the {@link Mediators} for this viewport.
     * Mediators can be used to e.g. to add zoom operations.
     * 
     * @return Mediators
     */
    public final Mediators getMediators()
    {
        return m_mediators;
    }

    /**
     * Add a mediator to the stack of {@link Mediators} for this viewport.
     * The one that is added last, will be called first.
     * 
     * Mediators can be used to e.g. to add zoom operations.
     * 
     * @param mediator IMediator
     */
    public final void pushMediator(final IMediator mediator)
    {
        m_mediators.push(mediator);
    }

    /**
     * Adds a ViewportTransformChangedHandler that will be notified whenever the Viewport's 
     * transform changes (probably due to a zoom or pan operation.)
     * 
     * @param handler ViewportTransformChangedHandler
     * @return HandlerRegistration
     */
    public HandlerRegistration addViewportTransformChangedHandler(final ViewportTransformChangedHandler handler)
    {
        return addEnsureHandler(ViewportTransformChangedEvent.getType(), handler);
    }

    public static class ViewportFactory extends ContainerNodeFactory<Viewport>
    {
        public ViewportFactory()
        {
            super(NodeType.VIEWPORT);

            // For Viewports, the Transform is required (for other Nodes it's optional),
            // so override the requirednesss.

            addAttribute(Attribute.TRANSFORM, true);
        }

        @Override
        public final Viewport container(final JSONObject node, final ValidationContext ctx) throws ValidationException
        {
            return new Viewport(node, ctx);
        }

        @Override
        public final boolean addNodeForContainer(final IContainer<?, ?> container, final Node<?> node, final ValidationContext ctx)
        {
            if (node.getNodeType() == NodeType.SCENE)
            {
                if (container.length() > 2)
                {
                    try
                    {
                        ctx.addError("Too many Scene objects is Viewport");
                    }
                    catch (ValidationException e)
                    {
                        return false;
                    }
                }
                container.asViewport().setSceneAndState(node.asScene());

                return true;
            }
            else
            {
                try
                {
                    ctx.addBadTypeError(node.getClass().getName() + " is not a Scene");
                }
                catch (ValidationException e)
                {
                    return false;
                }
            }
            return false;
        }
    }

    private static class DragLayer extends Layer
    {
        private DragContext2D m_context;

        public DragLayer()
        {
            setVisible(true);

            setListening(false);
        }

        @Override
        public CanvasElement getCanvasElement()
        {
            final CanvasElement element = super.getCanvasElement();

            if (null != element)
            {
                if (null == m_context)
                {
                    m_context = new DragContext2D(element);
                }
            }
            return element;
        }

        @Override
        public final Context2D getContext()
        {
            return m_context;
        }

        private static class DragContext2D extends Context2D
        {
            public DragContext2D(CanvasElement element)
            {
                super(element);
            }

            @Override
            public boolean isDrag()
            {
                return true;
            }
        }
    }
}
