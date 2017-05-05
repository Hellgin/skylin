<?php
	require_once "Component.php";
    class Table extends Component
    {
		private $view;
		private $viewE;
		private $columns = array();
		private $pageSize = 0;
		private $currentPage = 0;
		private $afterRowSelectE;
		private $afterColSelectE;
		private $userRowSelectionEnabledE;
		private $frozenCols = 0;
		private $frozenWidth;
		private $minFrozenWidth;
		private $rightPadding = 0;
		private $toolbar;
		private $page;
		
		private $rowStyleE;
		private $rowDraggableE;
		private $cellSStyleE;
	  
		function __construct()
		{
			$this->setProp('SKYLIN_TABLE', $this);
		}
		
		function setView($view)
		{
		  $this->view = $view;
		  return $this;
		}
		
		function setViewE($viewE)
		{
		  $this->viewE = $viewE;
		  return $this;
		}
		
		function getView()
		{
			return $this->view;
		}
		
		function setContent($name,$c)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new GridColumn($this);
			}
			$this->columns[$name]->setContent($c);
			$t = new TextField();
			$t->setValueE('return" '.$name.'";');
			$this->columns[$name]->setHeader($t);
			$this->columns[$name]->getHeader()->setKey($name);
			return $c;
		}
		
		function setHeader($name,$h)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new GridColumn($this);
			}
			$this->columns[$name]->setheader($h);
			return $h;
		}
	
		function setColRenderedE($name,$r)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new GridColumn($this);
			}
			$this->columns[$name]->setRenderedE($r);
		}
		
		function setWidth($name,$w)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new GridColumn($this);
			}
			$this->columns[$name]->setWidth($w);
		}
		
		function getContent($name)
		{
			return $this->columns[$name]->getContent();
		}
		
		function getColumn($name)
		{
			return $this->columns[$name];
		}
		
		function getLowestColumn($name)
		{
			foreach ($this->columns as $k => $c)
			{
				if ($c instanceof GridColumn)
				{
					if ($k == $name)
					{
						return $c;
					}
				}
				else 
				{
					$col = $c->getColumn($name);
					if ($col != null)
					{
						return $col;
					}
				}
			}
		}
		
		function setDynamicColE($name,$c)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new DynamicGridColumn($this);
			}
			$this->columns[$name]->setColE($c);
			return $c;
		}
		
		function setDynamicColHeaderTextE($name,$c)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new DynamicGridColumn($this);
			}
			$this->columns[$name]->setColHeaderTextE($c);
			return $c;
		}
	  
		function render()
		{
		  if (!is_null($this->viewE))
		  {
			  try
			  {
				$this->view = eval($this->viewE);
				if (java_is_null($this->view))
			    {
				  $this->view = null;
			    }
			  }
			  catch (Exception $e)
			  {
				  $this->view = null;
			  }
		  }

		  

		  $toolWidth = -1;
		  foreach($this->columns as $col)
		  {
		  	
		  	$col->setContext($this->getContext());
		  	$col->generateCols();
		  	if ($col->getRendered())
		  	{
		  		$toolWidth  = $toolWidth + $col->getWidth() + 1;
		  	}
		  }
		  if ($this->frozenWidth != null && $this->frozenWidth != -1 && $toolWidth > $this->frozenWidth)
		  {
		  	$toolWidth = $this->frozenWidth;
		  }
		  $toolbar = '';
		  if ($this->toolbar != null)
		  {
		  	$this->toolbar->setStyle("width:".$toolWidth);
		  	$toolbar = $this->toolbar->renderInContext($this->getContext());
		  }
		  
		  
		  if (!is_null($this->view))
		  {
		  	if ($this->pageSize > 0)
		  	{
		  		$rootRows= $this->view->getRowsBetweenForTable($this->pageSize*$this->currentPage,$this->pageSize*($this->currentPage+1),"unnamed");
		  		$rowCount = $this->view->getSizeForTable("unnamed");
		  	}
		  	else
		  	{
		  		$rootRows = $this->view->getRowsForTable("unnamed");
		  		$rowCount = count($rows);
		  	}
		  	$rows = array();
		  	foreach ($rootRows as $r)
		  	{
		  		array_push($rows,$r);
		  		$rows = array_merge($rows,$r->getFullTree("unnamed"));
		  		/*
		  		if ($r->isTreeExpanded())
		  		{
		  			$linkedRows = $r->getTreeLinkedRows("unnamed");
		  			if ($linkedRows != null)
		  			{
		  				$rows = array_merge($rows,$linkedRows);
		  			}
		  		}*/
		  	}
		  }
		  
		  

		  if ($this->pageSize > 0 && $this->view != null && $rowCount.'' > $this->pageSize.'')
		  {
		  	$pageControls = $pageControls.'<div class="page_control">';
		  	$pageControls = $pageControls.'<button id="'.$this->getFullId().'iback" type="submit" value="create" class = "grid_back_button"><img src="skylin/images/back.png"></button>';
		  	$a = (int)($rowCount.'');
		  	$a = $a - 1;
		  	$b = (int)($this->pageSize.'');
		  	$maxPage = (int)($a/$b);
		  	
		  	$dotStart = false;
		  	$dotEnd = false;
		  	for ($i = 1; $i <= $maxPage+1; $i++)
		  	{
		  		if ($i == 1 || $i == $maxPage+1 || ($i - $this->currentPage >= -2 && $i - $this->currentPage <= 4))
		  		{	
			  		$link = new TextField();
			  		$link->setValue($i);
			  		$link->setEditable(true);
			  		
			  		if ($this->currentPage+1 == $i)
			  		{
			  			$link->setId('link');
			  			$link->setStyle("width:25px; text-align: center; font-weight:900;");
			  			$link->setAction('$this->c("SKYLIN_TABLE")->setPage($this->a()->com("link")->getCurrentValue());
				                          $this->c("SKYLIN_TABLE")->refresh();');
			  		}
			  		else 
			  		{
			  			$link->link();
			  			$link->setAction('$this->c("SKYLIN_TABLE")->setPage('.$i.');
				                          $this->c("SKYLIN_TABLE")->refresh();');
			  		}
			  		
			  		$pageControls = $pageControls.'<div class="page_control_item">'.$link->renderInContext($this->getContext()).'</div>';
		  		}
		  		else
		  		{
		  			if ($i < $this->currentPage+1)
		  			{
		  				if(!$dotStart)
		  				{
		  					$pageControls = $pageControls.'...';
		  					$dotStart = true;
		  				}
		  			}
		  			elseif($i > $this->currentPage+1)
		  			{
		  				if(!$dotEnd)
		  				{
		  					$pageControls = $pageControls.'...';
		  					$dotEnd = true;
		  				}
		  			}
		  		}
			}
		  	$pageControls = $pageControls.'<button id="'.$this->getFullId().'inext" type="submit" value="create" class = "grid_next_button"><img src="skylin/images/next.png"></button>';
		  	$pageControls = $pageControls.'</div>';
		  }
		  
		  if ($this->frozenCols > 0)
		  {
			  $left = $this->renderCollsInRange($rows,$rowCount,0,$this->frozenCols-1,true);
			  $right = $this->renderCollsInRange($rows,$rowCount,$this->frozenCols,10000,false,'_right');

			  	$leftWidth = 0;
			    $i = -1;
			 
				foreach($this->columns as $col)
				{	
					$i = $i + 1;
					if ($i >= $this->frozenCols)
					{
						$rightWidth = $rightWidth + $col->getWidth();
						$notFrozen = $notFrozen + 1;
					}
					else 
					{
						$leftWidth = $leftWidth + $col->getWidth();
					}	
				}
			  	$totalWidth = $rightWidth+$leftWidth+$notFrozen+1;
			  	if ($this->frozenWidth != -1 && $this->frozenWidth < $totalWidth)
			  	{
			  		$totalWidth = $this->frozenWidth;
			  	}
			  	$rightWidth = $totalWidth - $leftWidth;
				return '<div id="'.$this->getFullId().'" style="width:100%" totalwidth='.$totalWidth.' leftwidth='.$leftWidth.' minrightwidth='.$this->minFrozenWidth.' paddingRight='.$this->rightPadding.' class="table_frozen">'.$toolbar.'<div style="display: table-row"><div style="display:table-cell">'.$left.$pageControls.'</div><div class="table_frozen_row" style="width:'.$rightWidth.'">'.$right.'</div></div></div>';  
			}		
			else 
			{
				return '<div id="'.$this->getFullId().'">'.$toolbar.$this->renderCollsInRange($rows,$rowCount,0,10000,false).$pageControls.'</div>';
			}  

		  
		}
		
		function renderCollsInRange($rows,$rowCount,$start, $end,$renderFrozenLine,$rowName = null)
		{
			$rowData = '';
			$this->handlePageLimit($rowCount);
			if ($rows != null)
			{
				foreach($rows as $row)
				{
					$rowData = $rowData.$this->renderRow($row,$start,$end,$renderFrozenLine,$rowName);
				}
			}
			$headingData = $this->renderHeader($start,$end,$renderFrozenLine);
			
			return '
			
			<div class="container">
				<div class="heading">
				'.$headingData.'
				</div>
				<div class="table-col">
				'.$rowData.'
				</div>
			</div>';			
		}
		
		function getFullRowId($row)
		{
			return $this->getFullId().'x'.$row->getLinkId();
		}
		
		function selectRow($row)
		{
			if (java_is_null($row))
			{
				return;
			}
			$old = $row->getView()->getCurrentRow();
			$row->getView()->setCurrentRow($row);
			//Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row));
			Response::addMessage('setprop',$this->getFullRowId($row).',class,table-row_selected');
			Response::addMessage('setprop',$this->getFullRowId($row).'_right,class,table-row_selected');
			
			if ($this->rowStyleE != null)
			{
				$oldTableLevelRow = $this->c('row');
				$this->setContextParam('row',$row);
				$rowStyle = eval($this->rowStyleE);
				$this->setContextParam('row',$oldTableLevelRow);
				
				Response::addMessage('setattr',$this->getFullRowId($row).',style,'.$rowStyle);
				Response::addMessage('setattr',$this->getFullRowId($row).'_right,style,'.$rowStyle);
			}
			
			if (!java_is_null($old))
			{
				//Response::addMessage('setdiv',$this->getFullRowId($old).'-'.$this->renderRow($old));
				Response::addMessage('setprop',$this->getFullRowId($old).',class,table-row');
				Response::addMessage('setprop',$this->getFullRowId($old).'_right,class,table-row');
				
				if ($this->rowStyleE != null)
				{
					$oldTableLevelRow = $this->c('row');
					$this->setContextParam('row',$old);
					$rowStyle = eval($this->rowStyleE);
					$this->setContextParam('row',$oldTableLevelRow);
				
					Response::addMessage('setattr',$this->getFullRowId($old).',style,'.$rowStyle);
					Response::addMessage('setattr',$this->getFullRowId($old).'_right,style,'.$rowStyle);
				}
			}
			if (!is_null($this->afterRowSelectE))
			{
				$this->setContextParam("SKYLIN_previousRow",$old);
				$this->setContextParam("SKYLIN_selectedRow",$row);
				eval($this->afterRowSelectE);
			}
		}
		
		function selectRowFromFrontEnd($link)
		{
			$row = $this->view->getRowByLinkId($link);
			if ($row != null && $row->isSelectable() && $this->getEnableUserRowSelection())
			{
				$this->selectRow($row);	
			}
		}
		
		function toggleRow($link)
		{
			$this->view->getRowByLinkId($link)->toggleTreeExpanded();
			$this->refresh();
		}
		
		function setEnableUserRowSelectionE($b)
		{
			$this->userRowSelectionEnabledE = $b;
			return $this;
		}
		
		function setEnableUserRowSelection($b)
		{
			$this->userRowSelectionEnabledE = 'return '.$b.';';
			return $this;
		}
		
		function getEnableUserRowSelection()
		{
			if ($this->userRowSelectionEnabledE != null)
			{
				return eval($this->userRowSelectionEnabledE);
			}
			return true;
		}
		
		function refreshRow($row)
		{
			if ($row == null)
			{
				return;
			}
			if ($this->frozenCols > 0)
			{
				Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row,0,$this->frozenCols-1,true));
				Response::addMessage('setdiv',$this->getFullRowId($row).'_right-'.$this->renderRow($row,$this->frozenCols,10000,false,'_right'));
			}
			else 
			{
				Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row,0,10000,true));
			}
		}
		
		function renderRow($row,$start,$end,$renderFrozenLine,$rowName = null)
		{
			$rowData = '';
		
			$rowContext = parent::getContext();	
			$rowContext['row'] = $row;
			$rowContext['skylin_firstCol'] = true;
			if ($row->isCurrentRow())
			{
				$class = 'table-row_selected';
			}
			else
			{
				$class = 'table-row';
			}
			
			if ($this->rowStyleE != null)
			{
				$oldTableLevelRow = $this->c('row');
				$this->setContextParam('row',$row);
				$rowStyle = ' style="'.eval($this->rowStyleE).'"';
				$this->setContextParam('row',$oldTableLevelRow);
			}
			$draggable = '';
			if (eval($this->rowDraggableE))
			{
				$draggable = 'draggable="true"';
				$rowContext['skylin_draggableContainer'] = $this->getFullRowId($row).$rowName;
			}
			$rowData = $rowData.'<div id = "'.$this->getFullRowId($row).$rowName.'" class="'.$class.'"'.$rowStyle.' '.$draggable.'>';
			$i = -1;
			foreach($this->columns as $key => $col)
			{
				$rowContext['col'] = $key;
				$i = $i + 1;
				if ($i == 1)
				{
					$rowContext['skylin_firstCol'] = false;
				}
				if ($i < $start || $i > $end)
				{
					continue;
				}
				if (!$col->getRendered())
				{
					continue;
				}
				if ($renderFrozenLine && $i == $end)
				{
					//$classes = "col last-frozen-col";
					$rowContext["skylin_renderFrozenLine"] = true;
				}
				else 
				{
					//$classes = "col";
					$rowContext["skylin_renderFrozenLine"] = false;
				}

				$rowData = $rowData.$col->renderInContext($rowContext);

			}
			$rowData = $rowData.'</div>';
			
			return $rowData;
		}
		
		function renderHeader($start,$end,$renderFrozenLine)
		{
					//<div class="col">category ID</div>
					//<div class="col">category </div>
			$headerData = '';
			$rowContext = parent::getContext();
			$i = -1;
			foreach($this->columns as $col)
			{
				$i = $i + 1;
				if ($i < $start || $i > $end)
				{
					continue;
				}
				if (!$col->getRendered())
				{
					continue;
				}
				if ($renderFrozenLine && $i == $end)
				{
					//$classes = "heading-col last-frozen-col";
					$rowContext['skylin_renderFrozenLine'] = true;
				}
				else 
				{
					//$classes = "heading-col";
					$rowContext['skylin_renderFrozenLine'] = false;
				}
				$rowContext['skylin_col'] = $col;
				//$headerData = $headerData.'<div class="'.$classes.'" style="min-width:'.$col->getWidth().'px;max-width:'.$col->getWidth().'px">';
				//$headerData = $headerData.'<div class="inner-col" style="min-width:'.($col->getWidth()-4).'px;max-width:'.($col->getWidth()-4).'px">';
				$headerData = $headerData.$col->getHeader()->renderInContext($rowContext);
				//$headerData = $headerData.'</div>';
				//$headerData = $headerData.'</div>';
			}
			
			return $headerData;
		}
		
		function refreshContentInRow($col,$row)
		{
			foreach($this->columns[$col]->getContentAsArray() as $c)
			{	
				$c->refreshInRow($row);
			}
		}
		
		function setPageSize($size)
		{
			$this->pageSize = $size;
		}
		
		function nextPage()
		{
			$this->currentPage = $this->currentPage  + 1;
			$this->handlePageLimit($this->view->getSizeForTable("unnamed"));
			$this->refresh();
		}
		function backPage()
		{
			//Response::info('back test');
			if ($this->currentPage  > 0)
			{
				$this->currentPage = $this->currentPage  - 1;
			}
			$this->refresh();
		}
		
		function handlePageLimit($rowCount)
		{
			if (java_is_null($this->view))
			{
				//$this->pageSize = 0;
				return;
			}
			if ($this->currentPage*$this->pageSize.'' > $rowCount.''-1)
			{
				$a = (int)($rowCount.'');
				$a  = $a  - 1;
				$b = (int)($this->pageSize.'');
				if ($b.''=='0')
				{
					$this->currentPage = 0;
				}
				else
				{
					$this->currentPage = (int)($a/$b);
				}
			}			
		}
		
		function setAutoBlockAction()
		{
		}
		
		function afterRowSelectE($e)
		{
			$this->afterRowSelectE = $e;
			return $this;
		}
		
		function afterColSelectE($e)
		{
			$this->afterColSelectE = $e;
			return $this;
		}
		
		function setFrozen($cols,$minFrozen = 0,$width = -1)
		{
			$this->frozenCols = $cols;
			$this->frozenWidth = $width;
			$this->minFrozenWidth = $minFrozen;
			return $this;
		}
		
		function setRowStyleE($s)
		{
			$this->rowStyleE = $s;
			return $this;
		}
		
		function setCellStyleE($s)
		{
			$this->cellStyleE = $s;
			return $this;
		}
		
		function getCellStyleE()
		{
			return $this->cellStyleE;
		}
		
		function setRightPadding($v)
		{
			$this->rightPadding = $v;
			return $this;
		}
		
		function setToolbar($tb)
		{
			$this->toolbar = $tb;
			return $tb;
		}
		
		function getToolbar()
		{
			return $this->toolbar;
		}
		
		function setPage($p)
		{
			if ($this->pageSize > 0)
			{
				if ($p <= 0)
				{
					$p = 1;
				}
				$this->currentPage = $p-1;
			}
		} 
		
		function doAfterColSelect()
		{
			if (!is_null($this->afterColSelectE))
			{
				eval($this->afterColSelectE);
			}
		}
		
		function dynamicColAfterValueChangeE($name,$e)
		{
			if ($this->columns[$name] == null)
			{
				$this->columns[$name] = new DynamicGridColumn($this);
			}
			$this->columns[$name]->dynamicColAfterValueChangeE($e);
		}
		
		function dragRow($from, $to)
		{
			if ($from != $to)
			{
				$this->view->getRowByLinkId($from)->moveTo($this->view->getRowByLinkId($to));
				$this->refresh();
			}
		}
		
		function setRowDraggableE($e)
		{
			$this->rowDraggableE = $e;	
		}
	}
  
  	class GridColumn extends Component
	{
		private $header;
		private $content;
		private $width = 100;
		private $table;
		
		public function __construct($table) 
		{	
			parent::__construct();
			$this->header = new GridHeading();
			$this->table = $table;
		}
		
		function setContent($c)
		{
			$this->content = $c;
		}
		
		function getContent()
		{
			return $this->content;
		}
		
		function getContentAsArray()
		{
			return [$this->content];
		}
		
		function setHeader($h)
		{
			$this->header->setHeader($h);
		}
		
		function getHeader()
		{
			return $this->header;
		}
		
		function getHeaderAsArray()
		{
			return [$this->header];
		}
		
		function render()
		{
			if ($this->c("skylin_renderFrozenLine"))
			{
				$classes = "col last-frozen-col";
			}
			else
			{
				$classes = "col";
			}

			if ($this->table->getCellStyleE() != null)
			{
				$cellStyle = eval($this->table->getCellStyleE());
			}
		
			$ret =      '<div class="'.$classes.'" style="min-width:'.$this->getWidthSingle().'px;max-width:'.$this->getWidthSingle().'px;'.$cellStyle.'">';
			$ret = $ret.'<div class="inner-col" style="min-width:'.($this->getWidthSingle()-4).'px;max-width:'.($this->getWidthSingle()-4).'px">';
			
			$content = $this->content->renderInContext(parent::getContext());
			if ($this->c('skylin_firstCol'))
			{
				if ($this->r()->isTreeExpandable("unnamed"))
				{
					if ($this->r()->isTreeExpanded())
					{
						$image = "item_expanded.png";
					}
					else 
					{
						$image = "item_collapsed.png";
					}
					$content = '<div id="'.$this->table->getFullRowId($this->c("row")).'_toggle" class="table-row_toggle" style="display: inline-block;padding-top:3px"><img src="skylin/images/'.$image.'"></div><div style="display: inline-block;vertical-align:top;width:calc(100% - 12px);float:right">'.$content.'</div>';
				}
				$depth = $this->r()->getTreeDepth();
				if ($depth > 0)
				{
					$content = '<div style="padding-left:'.($depth*20).'px">'.$content.'</div>';
				}
			}
			
			$ret = $ret.$content;
			
			$ret = $ret.'</div>';
			$ret = $ret.'</div>';
			return $ret;
		}
		
		function setWidth($width)
		{
			$this->width = $width;
		}
		
		function getWidth()
		{
			return $this->width;
		}
		
		function getWidthSingle()
		{
			return $this->width;
		}
		
		function getTable()
		{
			return $this->table;
		}
		
		function generateCols()
		{
			
		}
	}
	
	class GridHeading extends Component
	{
		private $header;
		private $key;
		private $draggable;
		
		function setHeader($h)
		{
			$this->header = $h;
		}
		
		function getHeader()
		{
			return $this->header;
		}
		
		function setKey($k)
		{
			$this->key = $k;
		}
		
		function getKey()
		{
			return $this->key;	
		}
		
		function render()
		{
			$classes = 'heading-col';
			if ($this->c('skylin_renderFrozenLine'))
			{
				$classes = $classes.' last-frozen-col';
			}
			if ($this->getKey() != null && $this->c("SKYLIN_TABLE")->getView() != null && $this->c("SKYLIN_TABLE")->getView()->getCurrentCol() == $this->getKey())
			{
				$classes = $classes.' heading-selected';
			}
			$col = $this->c('skylin_col');
			
			
			if ($this->draggable)
			{
				$draggable = 'draggable="true"';	
			}
			$ret = '<div id="'.$this->getFullId().'" class="'.$classes.'" style="min-width:'.$col->getWidthSingle().'px;max-width:'.$col->getWidthSingle().'px" '.$draggable.'>';
			$ret = $ret.'<div class="inner-col" style="min-width:'.($col->getWidthSingle()-4).'px;max-width:'.($col->getWidthSingle()-4).'px">';
			$ret = $ret.$this->header->renderInContext(parent::getContext());
			$ret = $ret.'</div>';
			$ret = $ret.'</div>';
			
			return $ret;
		}
		
		function select()
		{
			$id = $this->getKey();
			if (!$this->c("SKYLIN_TABLE")->getView()->isColSelectable($id))
			{
				return;
			}
			
			$old = $this->c("SKYLIN_TABLE")->getLowestColumn($this->c("SKYLIN_TABLE")->getView()->getCurrentCol());
			
			$o = $this->c("SKYLIN_TABLE")->getView()->getCurrentCol();
			if ($o == $id)
			{
				$this->c("SKYLIN_TABLE")->getView()->setCurrentCol(null);
			}
			else 
			{	
				$this->c("SKYLIN_TABLE")->getView()->setCurrentCol($id);
			}
			$this->refresh();
			
			if($old != null)
			{
				$old->getHeader()->refresh();
			}
			
			$this->c('SKYLIN_TABLE')->doAfterColSelect();
		}
		
		function makeDraggable()
		{
			$this->draggable = true;
		}
		

		function dragCol($to)
		{
			$this->c('SKYLIN_TABLE')->getView()->moveCol($this->key,$to->getKey());
			$this->c('SKYLIN_TABLE')->refresh();
		}
	}
	
	class DynamicGridColumn extends Component
	{
		private $table;
		private $header;
		private $colE;
		private $colHeaderTextE = 'return $this->colEResult;';
		private $colEResult;
		private $dynamicColAfterValueVhangeE;
		private $width = 100;
		
		private $colls = array();
		
		public function __construct($table)
		{
			parent::__construct();
			$this->header = new DynamicGridHeading();
			$this->table = $table;
		}
		
		function render()
		{
			$i = 0;
			foreach($this->colls as $key => $c)
			{
				$i = $i + 1;
				$context = $this->getContext();
				$context['col'] = $key;
				if ($i != count($this->colls))
				{
					$context['skylin_renderFrozenLine'] = false;
				}
				if ($this->c('skylin_firstCol') && $i > 1)
				{
					$context['skylin_firstCol'] = false;
				}
				$ret = $ret.$c->renderInContext($context);
			}
			return $ret;
		}
		
		function setColE($e)
		{
			$this->colE = $e;		
		}
		
		function setColHeaderTextE($e)
		{
			$this->colHeaderTextE = $e;
		}
		
		function setWidth($width)
		{
			$this->width = $width;
		}
		
		function getWidth()
		{
			return $this->width * count($this->colls) + count($this->colls) - 1;
		}
		
		function getWidthSingle()
		{
			return $this->width;
		}
		
		function getHeader()
		{
			return $this->header;
		}
		
		function generateCols()
		{
			$n = eval($this->colE);
			$match = true;
			if (count($n) != count($this->colEResult))
			{
				$match = false;
			}
			else 
			{
				for ($i = 0; $i < count($n);$i = $i + 1)
				{
					if ($n[$i] != $this->colEResult[$i])
					{
						$match = false;
						break 1;
					}
				}	
			}
			
					
			if (!$match)
			{
				$this->colEResult = $n;
				$headerText = eval($this->colHeaderTextE);
				$this->colls = array();
				$headers = array();
				
				for ($i = 0; $i < count($this->colEResult);$i = $i + 1)
				{
					$col = $this->colEResult[$i];
					$c = new GridColumn($this->table);
					$text = new TextField($col);
					$c->setContent($text);
					$text->afterValueChangeE($this->dynamicColAfterValueChangeE);
					$c->setWidth($this->width);
					$this->colls[$col] = $c;
						
					$h = $c->getHeader();
					$h->setHeader(new Label($headerText[$i]));
					$h->setKey($col);
					$h->makeDraggable();
					array_push($headers,$h);
				}
					
				$this->header->setHeaders($headers);
			}
		}
		
		function getContentAsArray()
		{
			$ret = array();
			foreach ($this->colls as $c)
			{
				array_push($ret,$c->getContent());
			}
			return $ret;
		}
		
		function getHeaderAsArray()
		{
			$ret = array();
			foreach ($this->colls as $c)
			{
				array_push($ret,$c->getHeader());
			}
			return $ret;
		}
		
		function getColumn($name)
		{
			return $this->colls[$name];
		}
		
		function dynamicColAfterValueChangeE($e)
		{
			$this->dynamicColAfterValueChangeE = $e;
		}
		
		function getTable()
		{
			return $this->table;
		}
	}
	
	class DynamicGridHeading extends Component
	{
		private $headers = array();
		
		function setHeaders($h)
		{
			$this->headers = $h;
		}
		
		function render()
		{
			$context = $this->getContext();
			foreach($this->headers as $c)
			{
				$i = $i + 1;
				$context = $this->getContext();
				if ($i != count($this->headers))
				{
					$context['skylin_renderFrozenLine'] = false;
				}
				$ret = $ret.$c->renderInContext($context);
			}
			return $ret;
		}
	}
	
	
if (isset($_POST['selectRow'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	 
	$s = explode('x',$_POST['selectRow'],5);
	//$s[3] for later use. grids inside iterators only partialy supported at this point.
	Application::getApplication($s[1])->getComponent($s[2])->selectRowFromFrontEnd($s[4]);
	Response::send();
}
else if (isset($_POST['next'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['next'],4);
	//$s[3] for later use. grids inside iterators only partialy supported at this point.
	//Response::info($s[1].'-'.$s[2]);
	Application::getApplication($s[1])->getComponent($s[2])->nextPage();
	Response::send();
}
else if (isset($_POST['back'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['back'],4);
	//$s[3] for later use. grids inside iterators only partialy supported at this point.
	//Response::info($s[1].'-'.$s[2]);
	Application::getApplication($s[1])->getComponent($s[2])->backPage();
	Response::send();
}

if (isset($_POST['toggleRow']))
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";

	$s = explode('x',$_POST['toggleRow'],5);
	Application::getApplication($s[1])->getComponent($s[2])->toggleRow($s[4]);
	Response::send();
}

if (isset($_POST['selectCol']))
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['selectCol'],5);
	Application::getApplication($s[1])->getComponent($s[2])->select();
	Response::send();
}

if (isset($_POST['dragRow']))
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$rows = explode(',',$_POST['dragRow'],2);
	$a = explode('x',$rows[0],5);
	$b = explode('x',$rows[1],5);
	Application::getApplication($a[1])->getComponent($a[2])->dragRow($a[4], $b[4]);
	Response::send();
}

if (isset($_POST['dragCol']))
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$rows = explode(',',$_POST['dragCol'],2);
	$a = explode('x',$rows[0],5);
	$b = explode('x',$rows[1],5);
	Application::getApplication($a[1])->getComponent($a[2])->dragCol(Application::getApplication($b[1])->getComponent($b[2]));
	Response::send();
}

?>