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
		private $userRowSelectionEnabled = true;
		private $frozenCols = 0;
		private $frozenWidth;
		private $minFrozenWidth;
		private $rightPadding = 0;
		private $toolbar;
		private $page;
		
		private $rowStyleE;
	  
		function __construct()
		{
			$this->setProp('SKYLIN_TABLE', $this);
		}
		
		function setView($view)
		{
		  $this->view = $view;
		}
		
		function setViewE($viewE)
		{
		  $this->viewE = $viewE;
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
	
		function setRenderedE($name,$r)
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
			  //return $this->view->getSize().'';
		  }


		  $toolWidth = -1;
		  foreach($this->columns as $col)
		  {
		  	$col->setContext($this->getContext());
		  	if ($col->getRendered())
		  	{
		  		$toolWidth  = $toolWidth + $col->getWidth() + 1;
		  	}
		  }
		  $toolbar = '';
		  if ($this->toolbar != null)
		  {
		  	$this->toolbar->setStyle("width:".$toolWidth);
		  	$toolbar = $this->toolbar->renderInContext($this->getContext());
		  }

		  if ($this->pageSize > 0 && $this->view != null && $this->view->getSize().'' > $this->pageSize.'')
		  {
		  	$pageControls = $pageControls.'<div class="page_control">';
		  	$pageControls = $pageControls.'<button id="'.$this->getFullId().'iback" type="submit" value="create" class = "grid_back_button">back</button>';
		  	$a = (int)($this->view->getSize().'');
		  	$a = $a - 1;
		  	$b = (int)($this->pageSize.'');
		  	$maxPage = (int)($a/$b);
		  	for ($i = 1; $i <= $maxPage+1; $i++)
		  	{
		  		$link = new TextField();
		  		$link->link();
		  		$link->setValue($i);
		  		$link->setEditable(true);
		  		$link->setAction('$this->c("SKYLIN_TABLE")->setPage('.$i.');
			                      $this->c("SKYLIN_TABLE")->refresh();');
		  		
		  		if ($this->currentPage+1 == $i)
		  		{
		  			$style = "text-decoration:underline;";
		  		}
		  		else 
		  			$style = null;
		  		
		  		$pageControls = $pageControls.'<div class="page_control_item" style = "'.$style.'">'.$link->renderInContext($this->getContext()).'</div>';
		  	}
		  	//$pageControls = $pageControls.'&nbsppage: '.($this->currentPage+1).'/'.($maxPage+1).'&nbsp';
		  	$pageControls = $pageControls.'<button id="'.$this->getFullId().'inext" type="submit" value="create" class = "grid_next_button">next</button>';
		  	$pageControls = $pageControls.'</div>';
		  }
		  	
		  
		  
		  if ($this->frozenCols > 0)
		  {
			  $left = $this->renderCollsInRange(0,$this->frozenCols-1,true);
			  $right = $this->renderCollsInRange($this->frozenCols,10000,false,'_right');

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
				return '<div id="'.$this->getFullId().'">'.$toolbar.$this->renderCollsInRange(0,10000,false).$pageControls.'</div>';
			}  

		  
		}
		
		function renderCollsInRange($start, $end,$renderFrozenLine,$rowName = null)
		{
			$rowData = '';
			$this->handlePageLimit();
			if (!is_null($this->view))
			{
				if ($this->pageSize > 0)
				{
					$rows = $this->view->getRowsBetween($this->pageSize*$this->currentPage,$this->pageSize*($this->currentPage+1));
				}
				else
				{
					$rows = $this->view->getRows();
				}
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
		
		function selectRow($link)
		{
			$row = $this->view->getRowByLinkId($link);
			if (java_is_null($row))
			{
				return;
			}
			$old = $this->view->getCurrentRow();
			$this->view->setCurrentRow($row);
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
				eval($this->afterRowSelectE);
			}
		}
		
		function selectRowFromFrontEnd($link)
		{
			if ($this->userRowSelectionEnabled)
			{
				$this->selectRow($link);	
			}
		}
		
		function setEnableUserRowSelection($b)
		{
			$this->userRowSelectionEnabled = $b;
		}
		
		function refreshRow($row)
		{
			if ($this->frozenCols > 0)
			{
				//out::println("refreshing!!");
				Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row,0,$this->frozenCols-1),true);
				Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row,$this->frozenCols,10000,false,'_right'));
			}
			else 
			{
				//out::println("refreshing!");
				Response::addMessage('setdiv',$this->getFullRowId($row).'-'.$this->renderRow($row,0,10000,true));
			}
		}
		
		function renderRow($row,$start,$end,$renderFrozenLine,$rowName = null)
		{
			$rowData = '';
		
			$rowContext = parent::getContext();	
			$rowContext['row'] = $row;
			if ($this->isCurrentRow($row))
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
			
			$rowData = $rowData.'<div id = "'.$this->getFullRowId($row).$rowName.'" class="'.$class.'"'.$rowStyle.'>';
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
					$classes = "col last-frozen-col";
				}
				else 
				{
					$classes = "col";
				}
				$rowData = $rowData.'<div class="'.$classes.'" style="min-width:'.$col->getWidth().'px;max-width:'.$col->getWidth().'px">';
				$rowData = $rowData.'<div class="inner-col" style="min-width:'.($col->getWidth()-4).'px;max-width:'.($col->getWidth()-4).'px">';
				$rowData = $rowData.$col->renderInContext($rowContext);
				$rowData = $rowData.'</div>';
				$rowData = $rowData.'</div>';
			}
			$rowData = $rowData.'</div>';
			
			return $rowData;
		}
		
		function isCurrentRow($row)
		{
			return !java_is_null($this->view->getCurrentRow()) && $row->getLinkId().'' == $this->view->getCurrentRow()->getLinkId().'';
		}
		
		function renderHeader($start,$end,$renderFrozenLine)
		{
					//<div class="col">category ID</div>
					//<div class="col">category </div>
			$headerData = '';
			
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
					//out::println('! '.$col->getRendered());
					continue;
				}
				if ($renderFrozenLine && $i == $end)
				{
					$classes = "heading-col last-frozen-col";
				}
				else 
				{
					$classes = "heading-col";
				}
				$headerData = $headerData.'<div class="'.$classes.'" style="min-width:'.$col->getWidth().'px;max-width:'.$col->getWidth().'px">';
				$headerData = $headerData.'<div class="inner-col" style="min-width:'.($col->getWidth()-4).'px;max-width:'.($col->getWidth()-4).'px">';
				$headerData = $headerData.$col->getHeader()->renderInContext(parent::getContext());
				$headerData = $headerData.'</div>';
				$headerData = $headerData.'</div>';
			}
			
			return $headerData;
		}
		
		function setPageSize($size)
		{
			$this->pageSize = $size;
		}
		
		function nextPage()
		{
			$this->currentPage = $this->currentPage  + 1;
			$this->handlePageLimit();
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
		
		function handlePageLimit()
		{
			if (java_is_null($this->view))
			{
				//$this->pageSize = 0;
				return;
			}
			if ($this->currentPage*$this->pageSize.'' > $this->view->getSize().''-1)
			{
				$a = (int)($this->view->getSize().'');
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
		}
		
		function setFrozen($cols,$minFrozen = 0,$width = -1)
		{
			$this->frozenCols = $cols;
			$this->frozenWidth = $width;
			$this->minFrozenWidth = $minFrozen;
		}
		
		function setRowStyleE($s)
		{
			$this->rowStyleE = $s;
		}
		
		function setRightPadding($v)
		{
			$this->rightPadding = $v;
		}
		
		function setToolbar($tb)
		{
			$this->toolbar = $tb;
			return $tb;
		}
		
		function setPage($p)
		{
			if ($this->pageSize > 0)
			{
				$this->currentPage = $p-1;
			}
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
		
		function setHeader($h)
		{
			$this->header->setHeader($h);
		}
		
		function getHeader()
		{
			return $this->header;
		}
		
		function render()
		{
			return $this->content->renderInContext(parent::getContext());
		}
		
		function setWidth($width)
		{
			$this->width = $width;
		}
		
		function getWidth()
		{
			return $this->width;
		}
		
		function getTable()
		{
			return $this->table;
		}
	}
	
	class GridHeading extends Component
	{
		private $header;
		function setHeader($h)
		{
			$this->header = $h;
		}
		
		function getHeader()
		{
			return $this->header;
		}
		
		function render()
		{
			return $this->header->renderInContext(parent::getContext());
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

?>