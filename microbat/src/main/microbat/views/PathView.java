package microbat.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import microbat.debugpilot.pathfinding.FeedbackPath;
import microbat.debugpilot.userfeedback.DPUserFeedback;
import microbat.model.trace.TraceNode;
import microbat.views.utils.lableprovider.FeedbackPathContentProvider;
import microbat.views.utils.lableprovider.FeedbackPathLabelProvider;
import microbat.views.utils.listeners.FeedbackPathSelectionListener;

public class PathView extends ViewPart {
	public static final String ID = "microbat.evalView.pathView";
	public static final String FOLLOW_PATH_FAMILITY_NAME = "Following Path";
	
	protected TraceView buggyTraceView = MicroBatViews.getTraceView();
	protected ReasonView reasonView = MicroBatViews.getReasonView();

	protected Text searchText;	
	protected Button searchButton;
	
	protected List<TableViewer> feedbackPathViewers = new ArrayList<>();
	protected List<FeedbackPath> feedbackPaths = new ArrayList<>();
	protected int pathID = -1;
	
	protected List<Button> checkButtons = new ArrayList<>();
	
	protected Job followPathJob = null;
	
	private Composite parent;
	private Composite tableComposite;
	
	public PathView() {
	}
	
	public void setSearchText(String expression) {		
		this.searchText.setText(expression);
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		
		parent.setLayout(new GridLayout(6,false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createSearchBox(parent);

		tableComposite = new Composite(parent, SWT.FILL | SWT.BORDER);
		tableComposite.setLayout(new GridLayout(1,true));
		GridData tableGridData = new GridData(SWT.FILL,SWT.FILL,true,true,6,1);
		tableComposite.setLayoutData(tableGridData);

		parent.layout();

	}
	
	/*
	 * Buttons
	 */
	private void createSearchBox(Composite parent) {
		searchText = new Text(parent, SWT.BORDER);
		searchText.setToolTipText("search trace node by class name and line number, e.g., ClassName line:20 or just ClassName\n"
				+ "press \"enter\" for forward-search and \"shift+enter\" for backward-search.");
		FontData searchTextFont = searchText.getFont().getFontData()[0];
		searchTextFont.setHeight(10);
		searchText.setFont(new Font(Display.getCurrent(), searchTextFont));
		searchText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		addSearchTextListener(searchText);

		searchButton = new Button(parent, SWT.PUSH);
		GridData buttonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		searchButton.setLayoutData(buttonData);
		searchButton.setText("Go");
		addSearchButtonListener(searchButton);		
		
		Button clearButton = new Button(parent, SWT.PUSH);
		GridData clearButtonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		clearButton.setLayoutData(clearButtonData);
		clearButton.setText("Clear");
		clearButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				clearFeedbackPath();
			}
		});
		
		Button playButton = new Button(parent, SWT.PUSH);
		GridData playButtonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		playButton.setLayoutData(playButtonData);
		playButton.setText("Follow Path");
		playButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (!feedbackPaths.isEmpty()) {
					Job job = new Job(PathView.FOLLOW_PATH_FAMILITY_NAME) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
						
							for (DPUserFeedback feedback : feedbackPaths.get(pathID).getFeedbacks()) {
								if (monitor.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								Display.getDefault().asyncExec(new Runnable() {
									@Override
									public void run() {
										focusOnNode(feedback.getNode());
									}
								});
								try {
									Thread.sleep(2500);
								} catch (InterruptedException e1) {
									e1.printStackTrace();
								}
								if (monitor.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
							}
							followPathJob = null;
							
							return Status.OK_STATUS;
						}
						
						@Override
						public boolean belongsTo(Object family) {
							return this.getName().equals(family);
						}
					};
					followPathJob = job;
					job.schedule();
				}
			}
		});
		
		Button stopPlayButton = new Button(parent, SWT.PUSH);
		GridData stopPlayButtonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		stopPlayButton.setLayoutData(stopPlayButtonData);
		stopPlayButton.setText("Stop Follow");
		stopPlayButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (followPathJob != null) {
					followPathJob.cancel();
					followPathJob = null;
				}
			}
		});
		
		Button deletePathButton = new Button(parent, SWT.PUSH);
		GridData deletePathButtonData = new GridData(SWT.FILL, SWT.FILL, false, false);
		deletePathButton.setLayoutData(deletePathButtonData);
		deletePathButton.setText("Del Path");
		deletePathButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				removeTable();
			}
		});
	}
	
	protected void addSearchButtonListener(final Button serachButton) {
		searchButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				String searchContent = searchText.getText();
				jumpToPath(searchContent);
			}
		});

	}
	
	public void changePathSelection(TableViewer tableViewer) {
		for(int i = 0;i<feedbackPathViewers.size();i++) {
			if(feedbackPathViewers.get(i) == tableViewer) {
				//changePathSelectionStyle(pathID,i);
				this.pathID = i;
				break;
			}
		}
	}
	
	public void changePathSelectionStyle(int prevID, int curID) {
		if(prevID == -1 || prevID >= feedbackPathViewers.size() || curID >= feedbackPathViewers.size()) {
			return;
		}
		Table curTable = feedbackPathViewers.get(curID).getTable();
		curTable.setBackground(new Color(Display.getCurrent(), 173, 255, 47));
	}
	
	public void otherViewsBehaviour(TraceNode node) {
		if (this.buggyTraceView != null) {
			this.buggyTraceView.jumpToNode(this.buggyTraceView.getTrace(), node.getOrder(), false);
			this.buggyTraceView.jumpToNode(node);
		}
		
		DPUserFeedback feedback = this.feedbackPaths.get(pathID).getFeedbackByNode(node);
		this.reasonView.refresh(feedback);
	}
	
	protected void addSearchTextListener(final Text searchText) {
		searchText.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 27 || e.character == SWT.CR) {
					String searchContent = searchText.getText();
					jumpToPath(searchContent);
				}
			}
		});

	}
	
	public void jumpToPath(final String pathIDStr) {
		try {
			int id = Integer.valueOf(pathIDStr);
			this.feedbackPathViewers.get(pathID).setSelection(new StructuredSelection(this.feedbackPathViewers.get(pathID).getElementAt(id)), true);
			this.feedbackPathViewers.get(pathID).refresh();
		} catch (NumberFormatException e) {
			// Do nothing
		}
	}
	
	
	/*
	 * Tables to show debugging plans
	 */
	private void createTableView(Composite parent) {
		Table table = new Table(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.horizontalSpan = 5;
		table.setLayoutData(gridData);
		
		TableColumn IdColumn = new TableColumn(table, SWT.LEFT);
		IdColumn.setAlignment(SWT.CENTER);
		IdColumn.setText("Path");
		IdColumn.setWidth(50);
		
		TableColumn TraceNodeColumn = new TableColumn(table, SWT.LEFT);
		TraceNodeColumn.setAlignment(SWT.CENTER);
		TraceNodeColumn.setText("Step");
		TraceNodeColumn.setWidth(100);
		
		TableColumn predictionColumn = new TableColumn(table, SWT.LEFT);
		predictionColumn.setAlignment(SWT.CENTER);
		predictionColumn.setText("Prediction");
		predictionColumn.setWidth(180);
		
		TableColumn confidenceColumn = new TableColumn(table, SWT.LEFT);
		confidenceColumn.setAlignment(SWT.CENTER);
		confidenceColumn.setText("Confidence");
		confidenceColumn.setWidth(120);
		
		TableColumn confirmColumn = new TableColumn(table, SWT.LEFT);
		confirmColumn.setAlignment(SWT.CENTER);
		confirmColumn.setText("Confirm");
		confirmColumn.setWidth(70);
		
		TableViewer firstViewer = new TableViewer(table);
		firstViewer.addPostSelectionChangedListener(new FeedbackPathSelectionListener(this));
		firstViewer.setContentProvider(new FeedbackPathContentProvider());
		
		feedbackPathViewers.add(firstViewer);
		
		tableComposite.setLayout(new GridLayout(1,true));
		tableComposite.layout();
	}
	
	public void addTable(FeedbackPath feedbackPath) {		
		Display.getDefault().syncExec(new Runnable() {
		    @Override
		    public void run() {		
				int columns = feedbackPathViewers.size()+1;
				tableComposite.setLayout(new GridLayout(columns,true));
		    	
				// add new table
				Table table = new Table(tableComposite, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
				table.setHeaderVisible(true);
				table.setLinesVisible(true);
				GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
				gridData.horizontalSpan = 1;
				table.setLayoutData(gridData);
				
			
				// add table column
				TableColumn dummyColumn = new TableColumn(table, SWT.FILL);
				dummyColumn.setAlignment(SWT.CENTER);
				dummyColumn.setText("");
				dummyColumn.setWidth(0);
				
				TableColumn TraceNodeColumn = new TableColumn(table, SWT.FILL);
				TraceNodeColumn.setAlignment(SWT.CENTER);
				TraceNodeColumn.setText("Node");
				
				TableColumn predictionColumn = new TableColumn(table, SWT.FILL);
				predictionColumn.setAlignment(SWT.CENTER);
				predictionColumn.setText("Prediction");

				// change column width
				table.addListener(SWT.Resize, new Listener() {
		            @Override
		            public void handleEvent(Event event) {
		                int width = table.getClientArea().width;
		                TableColumn column0 = table.getColumn(1);
		                column0.setWidth((int)(width*0.2));
		                TableColumn column1 = table.getColumn(2);
		                column1.setWidth((int)(width*0.8));
		            }
		        });
				
//				table.addListener(SWT.Paint, event -> {
//				    GC gc = event.gc;
//				    gc.setForeground(new Color(Display.getCurrent(), 173, 255, 47)); // 设置边框颜色
//				    gc.setLineWidth(2); // 设置边框宽度
//				    Rectangle rect = table.getBounds();
//				    gc.drawRectangle(rect.x, rect.y, rect.width - 1, rect.height - 1); // 绘制边框
//				});
				
				FeedbackPathSelectionListener listener = new FeedbackPathSelectionListener(PathView.this);
				TableViewer tableViewer = new TableViewer(table);
				tableViewer.addPostSelectionChangedListener(listener);
				tableViewer.setContentProvider(new FeedbackPathContentProvider());
				
				feedbackPaths.add(feedbackPath);
				feedbackPathViewers.add(tableViewer);

				tableViewer.setLabelProvider(new FeedbackPathLabelProvider(feedbackPath));
				tableViewer.setInput(feedbackPath);
				tableViewer.refresh();
				checkButtons.clear();
					
				table.layout();
				tableComposite.layout();
				parent.layout();
		    }
		});
	}
	
	public void removeTable() {
		if(pathID == -1 || pathID >= feedbackPaths.size()) {
			return;
		}
		Table selectTable = feedbackPathViewers.get(pathID).getTable();
		selectTable.dispose();
		feedbackPaths.remove(pathID);
		feedbackPathViewers.remove(pathID);
		if(pathID!=0) {
			pathID = pathID-1;
			//TODO change selection
		}
		int columns = feedbackPathViewers.size();
		tableComposite.setLayout(new GridLayout(columns,true));
		tableComposite.layout();
	}
	
	@Override
	public void setFocus() {

	}
	
	public void updateFeedbackPath(final FeedbackPath feedbackPath) {
		Display.getDefault().syncExec(new Runnable() {
		    @Override
		    public void run() {
		    	if(feedbackPathViewers.isEmpty()) {
		    		pathID = 0;
		    		addTable(feedbackPath);
		    	}
		    	else {
		    		feedbackPaths.set(pathID, feedbackPath);
			        feedbackPathViewers.get(pathID).setLabelProvider(new FeedbackPathLabelProvider(feedbackPath));
			        feedbackPathViewers.get(pathID).setInput(feedbackPath);
			        feedbackPathViewers.get(pathID).refresh();
			        checkButtons.clear();		    	
			    }
		    }
		});
	}
	
	public void updateFeedbackPaths(final List<FeedbackPath> paths) {
		// first path: add or substitute
		FeedbackPath firstPath  = paths.get(0);
		
		if(feedbackPaths.size() == 0) {
			addTable(firstPath);
			pathID = 0;
		}
		else {
			Display.getDefault().syncExec(new Runnable() {
			    @Override
			    public void run() {
		    		feedbackPaths.set(pathID, firstPath);
			        feedbackPathViewers.get(pathID).setLabelProvider(new FeedbackPathLabelProvider(firstPath));
			        feedbackPathViewers.get(pathID).setInput(firstPath);
			        feedbackPathViewers.get(pathID).refresh();
			        checkButtons.clear();
			    }
			});
		}
		
		// other paths: add
		for(int i = 1; i < paths.size();i++) {
			addTable(paths.get(i));
		}
	}
	
	public void clearFeedbackPath() {
		for(Control table : tableComposite.getChildren()) {
			table.dispose();
		}
		feedbackPaths.clear();
		feedbackPathViewers.clear();
		pathID = -1;
	}
	
	public void setBuggyView(TraceView view) {
		this.buggyTraceView = view;
	}
	
	public FeedbackPath getFeedbackPath() {
		if(pathID == -1) {
			return null;
		}
		return this.feedbackPaths.get(pathID);
	}
	
	public void focusOnNode(final TraceNode node) {
		if (node == null) return;
		for (DPUserFeedback feedback : this.feedbackPaths.get(pathID).getFeedbacks()) {
			if (feedback.getNode().equals(node)) {
				StructuredSelection selection = new StructuredSelection(feedback);
				this.feedbackPathViewers.get(pathID).setSelection(selection);
				break;
			}
		}
	}
	
}
