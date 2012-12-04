/**
 * 
 */
package org.semanticsoft.vaaclipsedemo.mediaplayer.views;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.extensions.EventUtils;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.semanticsoft.vaaclipsedemo.mediaplayer.constants.MediaConstants;
import org.semanticsoft.vaaclipsedemo.mediaplayer.model.Media;
import org.semanticsoft.vaaclipsedemo.mediaplayer.model.MediaCategory;
import org.semanticsoft.vaaclipsedemo.mediaplayer.model.MediaLibrary;
import org.semanticsoft.vaaclipsedemo.mediaplayer.service.MediaService;

import com.vaadin.data.Container;
import com.vaadin.data.Container.Hierarchical;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.Tree.TreeDragMode;
import com.vaadin.ui.VerticalLayout;

/**
 * @author rushan
 *
 */
public class MediaLibraryView {
	
	private static final String NAME_PROP = "name";
	private static final String ICON_PROP = "icon";
	private static final String OBJECT_PROP = "object";
	
	private Panel panel;
	private Tree tree;

	@Inject
	MediaLibrary mediaLibrary;
	
	@Inject
	EModelService modelService;
	
	@Inject
	MApplication app;
	
	@Inject
	IEventBroker broker;
	
	@Inject
	MediaService mediaService;
	
	private Hierarchical container;
	
	private EventHandler mediaChangedHandler = new EventHandler() {
		
		@Override
		public void handleEvent(Event event) {
			Object data = event.getProperty(EventUtils.DATA);
			if (data instanceof Media){
				Media media = (Media) data;
				String id = mediaService.getId(media);
				Item item = container.getItem(id);
				item.getItemProperty(NAME_PROP).setValue(media.getName());
			}
		}
	};
	
	@PostConstruct
	public void postConstruct(VerticalLayout parent, IEclipseContext context)
	{
		panel = new Panel();
		panel.setSizeFull();
		parent.addComponent(panel);
		
		createMediaLibraryTree();
		
		broker.subscribe(MediaConstants.mediaChanged, mediaChangedHandler);
	}
	
	@PreDestroy
	public void preDestory()
	{
		broker.unsubscribe(mediaChangedHandler);
	}
	
	private void createMediaLibraryTree()
	{
		tree = new Tree();
		tree.setDragMode(TreeDragMode.NODE);
		tree.setSizeFull();
		tree.setImmediate(true);
		panel.addComponent(tree);

		container = createMediaLibraryDataSource();
		tree.setContainerDataSource(container);

		tree.addListener(new ItemClickEvent.ItemClickListener() {

			private static final long serialVersionUID = 1L;

			public void itemClick(final ItemClickEvent event)
			{
				if (event.getButton() == ItemClickEvent.BUTTON_LEFT)
				{
					Item item = event.getItem();
					Object object = item.getItemProperty(OBJECT_PROP).getValue();
					if (object != null && object instanceof Media)
					{
						Media media = (Media)object;
						
						mediaLibrary.setSelectedMedia(media);
						broker.send(MediaConstants.mediaSelected, media);
					}
				}
			}
		});
		
		// Set tree to show the 'name' property as caption for items
		tree.setItemCaptionPropertyId(NAME_PROP);
		tree.setItemIconPropertyId(ICON_PROP);
		
		// Expand whole tree
		for (Object id : tree.rootItemIds())
		{
			tree.expandItemsRecursively(id);
		}
	}
		
	private Container.Hierarchical createMediaLibraryDataSource()
	{
		HierarchicalContainer container = new HierarchicalContainer();
		container.addContainerProperty(NAME_PROP, String.class, "No Name");
		container.addContainerProperty(ICON_PROP, ThemeResource.class, null);
		container.addContainerProperty(OBJECT_PROP, Object.class, null);
		fillContainer(mediaLibrary, null, "", container);
		return container;
	}
	
	private void fillContainer(MediaCategory category, Item categoryItem, String categoryPath, HierarchicalContainer container)
	{
		for (MediaCategory childCategory : category.getCategories())
		{
			String childCategoryPath = categoryPath + "/" + childCategory.getName();
			Item childCategoryItem = container.addItem(childCategoryPath);
			if (!(category instanceof MediaLibrary))
				container.setParent(childCategoryPath, categoryPath);
			childCategoryItem.getItemProperty(NAME_PROP).setValue(childCategory.getName());
			childCategoryItem.getItemProperty(ICON_PROP).setValue(new ThemeResource("org.semanticsoft.vaaclipsedemo.mediaplayer/icons/mediacategory.png"));
			childCategoryItem.getItemProperty(OBJECT_PROP).setValue(childCategory);
			fillContainer(childCategory, childCategoryItem, childCategoryPath, container);
		}
		
		for (Media media : category.getMediaList())
		{
			String mediaPath = categoryPath + "/" + media.getUri();
			Item mediaItem = container.addItem(mediaPath);
			if (!(category instanceof MediaLibrary))
				container.setParent(mediaPath, categoryPath);
			container.setChildrenAllowed(mediaPath, false);
			mediaItem.getItemProperty(NAME_PROP).setValue(media.getName());
			mediaItem.getItemProperty(ICON_PROP).setValue(new ThemeResource("org.semanticsoft.vaaclipsedemo.mediaplayer/icons/media.png"));
			mediaItem.getItemProperty(OBJECT_PROP).setValue(media);
		}
	}
	
	public Tree getTree()
	{
		return tree;
	}
}
