<?php
	require_once "Component.php";
	class TabStrip extends Component
	{
		private $currentTab;
		private $mode = 1;
		private $tabcreateSeqNo = 0;
		private $tabLookupBySeq = array();
		
		function render()
		{
			$tabStripId = $this->getFullId();
			$i = 0;
			$childrenContext = $this->getContext();
			$childrenContext['parent'] = $this;
			foreach($this->tabLookupBySeq as $tab)
			{
				if ($this->currentTab == $tab)
				{
					$extra = $extra.'checked="checked"';
				}
				else
				{
					$extra = '';
				}
				if ($this->mode == 1 || $this->currentTab == $tab)
				{
					$content = $tab->getContent()->renderInContext($childrenContext);
				}
				else
				{
					$content = '';
				}
				//$heading = str_replace(" ","_",$tab->getHeading());
				$seq = $tab->getSeqNo();
				if ($tab->getCloseable())
				{
					$closeButton = '<button id="'.$tabStripId.'x'.$seq.'xclose" type="submit" class="tab_close">
										<img src="skylin/images/tab_close.png">
									</button>';
					$labelStyle = 'style="padding: 0 0 0 10px;"';
				}
				$labelId = $tabStripId.'x'.$seq.'xlabel';
				if (!eval($tab->getEditableE()))
				{
					$disabled = ' disabled';
				}
				else 
				{
					$disabled = '';
				}
				$tabsData = $tabsData.'
				<li>
					<input type="radio"'.$disabled.' class="tab"'.$extra.' name="'.$tabStripId.'" id="'.$tabStripId.'x'.$seq.'" /><label '.$labelStyle.' class="tab_label" for="'.$tabStripId.'x'.$seq.'"><div id="'.$labelId.'" style="display:inline">'.$tab->getHeading().'</div>'.$closeButton.'</label>
					<div id="'.$tabStripId.'_'.$seq.'"  style="overflow:hidden">
						'.$content.'
					</div>
				</li>';
				$i=$i+1;
			}
		
			if (!is_null($this->getStyle()))
			{
				$style =$this->getStyle();
			}
			
			if (!is_null($style))
			{
				$extra = 'style="'.$style.'"';
			}

		
			$ret = 
			'<div id="'.$tabStripId.'" class="css3-tabstrip" '.$extra.'>
				<ul>
					'.$tabsData.'
				</ul>
			</div>';

			return $ret;
		}
		
		function setTab($heading,$content)
		{
			
			$old = $this->getTabContent($heading);
			if ($old != null)
			{
				$old->destroy();
			}
			
			if (!is_null($this->a()))//not 100% sure this check is needed here. test later.
			{
				$childrenContext = $this->getContext();
				$childrenContext['parent'] = $this;
				$renderedContent = $content->renderInContext($childrenContext);
			}
			
			$index = $this->getTabIndex($heading);
			if ($index == -1)
			{
				$tab = new Tab();
				$content->setProp('TAB_STRIP_TAB_HEADING',$heading);
				$content->setProp('TAB_STRIP_TAB_NO',$this->tabcreateSeqNo);
				$this->tabLookupBySeq[$this->tabcreateSeqNo] = $tab;
				$tab->setSeqNo($this->tabcreateSeqNo);
				$tabSeq = $this->tabcreateSeqNo;
				$tab->setTabStrip($this);
				$this->tabcreateSeqNo = $this->tabcreateSeqNo + 1;
				
				$existingTab = false;
				//array_push($this->tabs,$tab);
			}
			else 
			{
				$tabSeq = $this->getTabAtIndex($index)->getSeqNo();
				//$this->tabLookupBySeq[$tabSeq] = $tab;
				$tab = $this->tabLookupBySeq[$tabSeq];
				$content->setProp('TAB_STRIP_TAB_NO',$tabSeq);
				
				$existingTab = true;
				//$this->tabs[$index] = $tab;
			}

			$tab->setHeading($heading);
			$tab->setContent($content);
			
			if (!is_null($this->a()))
			{
				Response::addMessage('setprop',$this->getFullId().'x'.str_replace(" ","_",$this->currentTab->getSeqNo()).',checked,');
			}
			$this->currentTab = $tab;
			
			if (!is_null($this->a()))
			{
				$seq = $tab->getSeqNo();
				$tabStripId = $this->getFullId();
				if ($tab->getCloseable())
				{
					$closeButton = '<button id="'.$tabStripId.'x'.$seq.'xclose" type="submit" class="tab_close">
										<img src="skylin/images/tab_close.png">
									</button>';
					$labelStyle = 'style="padding: 0 0 0 10px;"';
				}	
				else {
					$closeButton = '';
					$labelStyle = '';
				}
				$labelId = $tabStripId.'x'.$seq.'xlabel';
				$extra = $extra.'checked="checked"';
				if (!eval($tab->getEditableE()))
				{
					$disabled = ' disabled';
				}
				else
				{
					$disabled = '';
				}
				$t = '<li>
						<input type="radio"'.$disabled.' class="tab"'.$extra.' name="'.$tabStripId.'" id="'.$tabStripId.'x'.$seq.'" /><label '.$labelStyle.' class="tab_label" for="'.$tabStripId.'x'.$seq.'"><div id="'.$labelId.'" style="display:inline">'.$tab->getHeading().'</div>'.$closeButton.'</label>
						<div id="'.$tabStripId.'_'.$seq.'"  style="overflow:hidden">
							'.$renderedContent.'
						</div>
					</li>';
				if ($existingTab)
				{
					Response::addMessage('setparentdiv',$tabStripId.'x'.str_replace(" ","_",$this->currentTab->getSeqNo()).'-'.$t);	
					Response::addMessage('setprop',$this->getFullId().'x'.str_replace(" ","_",$this->currentTab->getSeqNo()).',checked,checked');
				}
				else 
				{
					Response::addMessage('appendtofirstchild',$this->getFullId().'-'.$t);
				}
			}
			return $content;
		}
		
		function getTabContent($heading)
		{
			$tab = $this->getTabAtIndex($this->getTabIndex($heading));
			if ($tab != null)
			{
				return $tab->getContent();
			}
			return null;
		}
		
		function removeTabBySeq($seq)
		{
			$tabToRemove = $this->tabLookupBySeq[$seq];
			$index = $this->getTabIndexBySeq($seq);
			
			$tabToRemove->getContent()->destroy();//temp. should be controlable by developer.
			
			Response::addMessage('setparentdiv',$this->getFullId().'x'.str_replace(" ","_",$seq).'-');

			unset($this->tabLookupBySeq[$seq]);

			if ($tabToRemove == $this->currentTab)
			{
				if ($index >= count($this->tabLookupBySeq))
				{
					$index = $index - 1;
				}
				$this->currentTab = $this->getTabAtIndex($index);
			}
	
			Response::addMessage('setprop',$this->getFullId().'x'.str_replace(" ","_",$this->currentTab->getSeqNo()).',checked,checked');
		}
		
		function getTabIndex($heading)
		{
			$i = 0;
			foreach($this->tabLookupBySeq as $key => $tab)
			{
				if ($heading == $tab->getHeading())
				{
					return $i;
				}
				$i  = $i  + 1;
			}
			return -1;
		}
		
		function getTabIndexBySeq($seq)
		{
			$i = 0;
			foreach($this->tabLookupBySeq as $tab)
			{
				if ($seq == $tab->getSeqNo())
				{
					return $i;
				}
				$i  = $i  + 1;
			}
		}
		
		function getTabAtIndex($index)
		{
			foreach($this->tabLookupBySeq as $key => $tab)
			{
				if ($i == $index)
				{
					return $tab;
				}
				$i  = $i  + 1;
			}
		}
		
		function getTabBySeq($no)
		{
			return $this->tabLookupBySeq[$no];
		}
		
		function changeTab($seq)
		{
			$this->currentTab = $this->tabLookupBySeq[$seq];
			if ($this->mode == 0 && !is_null($this->a()))
			{
				$this->refresh();
			}
		}
		
		function partialRenderMode()
		{
			$this->mode = 0;
		}
		
		function completeRenderMode()
		{
			$this->mode = 1;
		}
		
		function setTabApplication($heading,$app)
		{
			$this->setTab($heading,$this->a()->newChildApplication($app));
		}
		
		function setCloseable($h,$c)
		{
			$this->tabLookupBySeq[$this->getTabIndex($h)]->setCloseable($c);
		}
		
		function setCurrentTabIndex($index)
		{
			$this->currentTab = $this->getTabAtIndex($index);
		}
		
		function setTabEditableE($heading,$e)
		{
			$this->getTabAtIndex($this->getTabIndex($heading))->setEditableE($e);
		}
	}
	
	class Tab
	{
		private $heading;
		private $content;
		private $closeable = true;
		private $seqNo;
		private $tabStrip;
		private $editableE = 'return true;';
		
		function setHeading($h)
		{
			$this->heading = $h;
		}
		function setContent($c)
		{
			$this->content = $c;
		}
		function getHeading()
		{
			return $this->heading;
		}
		function getContent()
		{
			return $this->content;
		}
		function setCloseable($c)
		{
			$this->closeable = $c;
		}
		function getCloseable()
		{
			return $this->closeable;
		}
		
		function setSeqNo($no)
		{
			$this->seqNo = $no; 
		}
		
		function getSeqNo()
		{
			return $this->seqNo;
		}
		
		function setTabStrip($tabs)
		{
			$this->tabStrip = $tabs;
		}
		
		function setEditableE($e)
		{
			$this->editableE = $e;
		}
		
		function getEditableE()
		{
			return $this->editableE;
		}
		
		function rename($newName)
		{
			$this->heading = $newName; 	
			$labelId = $this->tabStrip->getFullId().'x'.$this->seqNo.'xlabel';
			Response::addMessage('setdiv',$labelId.'-<div id="'.$labelId.'" style="display:inline">'.$newName.'</div>');
		}
	}
	
if (isset($_POST['changetab'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['changetab'],5);
	//$s[3] not used yet.
	Application::getApplication($s[1])->getComponent($s[2])->changeTab(str_replace("_"," ",$s[4]));
	Response::send();
}	

if (isset($_POST['close'])) 
{
	require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
	$s = explode('x',$_POST['close'],6);
	//$s[3] not used yet.
	//Response::info($s[1]);
	Application::getApplication($s[1])->getComponent($s[2])->removeTabBySeq(str_replace("_"," ",$s[4]));
	Response::send();
}	


	
?>