<?php
	require_once "Lov.php";
    class SearchLov extends Lov
    {
    	private $doInListValidationE = 'return $this->getLov()->doInListValidation();';
    	private $textStyleE;
    	
		function render()
		{
			if (!$this->getEditable())
			{
				return parent::render();
			}
			return '<div class="searchLov" id="'.$this->getFullId().'_lov" style="'.parent::getStyle().'">
						<div class="nestedTextField">'.parent::render().'</div>
						<button class="search_lov_button" id="'.$this->getFullId().'xlov_button"><img src="skylin/images/search.png"></button>
						<div class="search_lov_popup" id="'.$this->getFullId().'_lov_popup"></div>
					</div>';
		}
		
		function getStyle()
		{
			if ($this->textStyleE != null)
			{
				return eval($this->textStyleE);
			}
			return '';
		}
		
		function searchLovClick($id)
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($id));
			Response::addMessage('setdiv',$this->getFullId().'_'.'lov_popup-'.$this->renderPopup());
			Response::addMessage('setprop',$this->getFullId().'_lov_dialog,class,modalDialog modalDialog-open');
		}// modalDialog-open
		
		function renderPopup()
		{
			$lov = $this->getLov();
			$rows = $lov->getRows();
			
			$table = $table.'<div class="container">';
			foreach($lov->getUserFilterColumns() as $filter)
			{
				$table = $table.'<div class="table-row" style="border-style:none">';
				$table = $table.'<div class="col" style="border-style:none;font-size:0.85em;white-space:nowrap"><div>'.$filter->getFilterLabel().'</div></div>';
				$table = $table.'<div class="col" style="width:100%;padding-left: 5;border-style:none;font-size:0.85em"><input class ="searchLovFilter" filter="'.$filter->getFilterColumn().'" style="width:100%;"></div>';
				$table = $table.'</div>';
			}
			$table = $table.'</div>';
			
			foreach($lov->getDisplayColumns() as $col)
			{
				$table = $table.'<div style="width: '.$col->getDisplayWidth().'; min-width: '.$col->getDisplayWidth().'; max-width: '.$col->getDisplayWidth().';display: table-cell;font-weight: bold;color: rgb(0,60,91);font-size:0.9em">'.$col->getDisplayLabel().'</div>';
				$cols=$cols.$col->getDisplayColumn().',';
			}
			if (strlen($cols) > 0)
			{
				$cols = substr($cols,0,strlen($cols) - 1);
			}
			$table = $table.'<div style="max-height: 60vh;overflow-y:auto;overflow-x:hidden;">';
			$table = $table.'<div class="search_lov_container" cols="'.$cols.'">';
			foreach ($rows as $row)
			{
				$table = $table.'<div id="'.$this->getFullId().'X'.$row->getLinkId().'" class="search_lov_table-row">';
				foreach($lov->getDisplayColumns() as $col)
				{
					$table = $table.'<div class="search_lov_col" style="width: '.$col->getDisplayWidth().'; min-width: '.$col->getDisplayWidth().'; max-width: '.$col->getDisplayWidth().'">'.$row->getValue($col->getDisplayColumn()).'</div>';
				}
				
				$table = $table.'</div>';
			}
			$table = $table.'</div>';
			$table = $table.'</div>';
			
			//$table=$table.'<button class="search_lov_close"><img src="'.$_SESSION['WEB_APP'].'/skylin/images/tab_close.png"></button>';
			$table=$table.'<button class="search_lov_close"><img src="skylin/images/tab_close.png"></button>';
			
			return '<div class="search_lov_popup" id="'.$this->getFullId().'_lov_popup">
						<div id="'.$this->getFullId().'_lov_dialog" class="modalDialog">
							<div style="width: '.$lov->getWidth().';">
									'.$table.'
							</div>
						</div>
					</div>';
		}
		
		function refresh()
		{
			//Response::addMessage('setdiv',$this->getFullId().'_lov-'.$this->render());
			Response::addMessage('setdiv',$this->getFullId().'_lov-'.$this->renderInContext($this->getContext()));
		}
		
		function valueChange_step1($linkId,$value)
		{
			/*
			if (eval($this->doInListValidationE))
			{
				$this->setRow($linkId);
				$lov = $this->getLov();
				$toBeSelected = $lov->findRowByUserInput($this->c('col'),$value);
				if (java_is_null($toBeSelected) && !(is_null($value) || strlen($value.'') == 0))
				{
					$this->setError('Value not in list',$this->getFullId());
					return;
				}
				if (!java_is_null($toBeSelected))
				{
					$this->selectRow($linkId,$toBeSelected->getLinkId());
				}
				else
				{
					$this->selectRow($linkId,null);
				}
			}
			else
			{
				parent::valueChange($linkId,$value);
			}
			*/
			
			$this->setRow($linkId);
			if (eval($this->doInListValidationE))
			{
				$lov = $this->getLov();
				$toBeSelected = $lov->findRowByUserInput($this->c('col'),$value);
				if (java_is_null($toBeSelected) && !(is_null($value) || strlen($value.'') == 0))
				{
					$this->setError('Value not in list',$this->getFullId());
					return false;
				}
			}
			
			return parent::valueChange_step1($linkId,$value);
		}
		
		
		function selectRow($rowId,$lovRowId)
		{
			$_REQUEST['SKYLIN_LOV_ROW_SELECTED'] = true;
			parent::selectRow($rowId,$lovRowId);
		}
		
		function setError($result,$id)
		{
			if ($_REQUEST['SKYLIN_LOV_ROW_SELECTED'])
			{
				if ($result != null)
				{
					Response::error($result);
				}
			}
			else
			{
				parent::setError($result,$id);
			}
		}
		
		function setInListValidation($v)
		{
			$this->doInListValidationE = 'return ' || $v || ';';
			return $this;
		}
		
		function setInListValidationE($v)
		{
			$this->doInListValidationE = $v;
			return $this;
		}		

		function setTextStyle($s)
		{
			$this->textStyleE = 'return "'.$s.'";';
			return $this;
		}
		function setTextStyleE($s)
		{
			$this->textStyleE = $s;
			return $this;
		}
	}
	
	if (isset($_POST['searchLovClick'])) 
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php"; 
		$s = explode('x',$_POST['searchLovClick'],5);
		Application::getApplication($s[1])->getComponent($s[2])->searchLovClick($s[3]);
		Response::send();
	}
	
	if (isset($_POST['selectLovRow'])) 
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
		$ss = explode('X',$_POST['selectLovRow'],2);
		$s = explode('x',$ss[0],4);
		$com = Application::getApplication($s[1])->getComponent($s[2]);
		$com->selectRow($s[3],$ss[1]);
		$com->refresh();
		Response::send();
	}
?>