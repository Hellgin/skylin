<?php
	require_once "Component.php";
	class File extends Component
	{
		private $afterValueChangeE;
		private $editableE = 'return $this->r()->isUpdateable($this->c("col"));';
		private $uploadRenderedE = 'return true;';
		private $downloadRenderedE = 'return $this->r()->getValue($this->c("col"))->size() > 0;';
		
		public function __construct($autoSet = null,$autoSetName = null,$rowVarName = null)
		{
			parent::__construct();
			if ($autoSet != null)
			{
				$this->setProp("col",$autoSet);
			}
			if ($rowVarName != null)
			{
				$this->setRowVarName($rowVarName);
			}
			if ($autoSet != null)
			{
				$this->setProp("col_name",$autoSetName);
			}
		}
		
		function render()
		{
			if (!is_null($this->editableE) && !eval($this->editableE))
			{
				$dis = 'disabled';
			}
			if (eval($this->downloadRenderedE))
			{
				$download = '<button id='.$this->getFullId().'_download class="file_download" style="background-image:url(skylin/images/download.png)">download</button></a>';
			}
			if (eval($this->uploadRenderedE))
			{
				$upload = '<button class="file_upload_trigger" style="background-image:url(skylin/images/upload.png)" '.$dis.'>upload</button>';
			}

			return '<div id='.$this->getFullId().' urltodownload= "">
					<input type=file class="file_upload" value="upload file" style="display:none">
					'.$upload.'
					'.$download.'
					</div>';
		}
		
		function fileChange($linkId,$fileLocation,$fileName)
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($linkId));

			//Response::info($_FILES['FILE_UPLOAD_FILE']['tmp_name']);
			//Response::info($_FILES['FILE_UPLOAD_FILE']['name']);
			
			$error = $this->r()->setFileFromFrontEnd($this->c('col'),$_FILES['FILE_UPLOAD_FILE']['tmp_name'],$this->c('col_name'),$_FILES['FILE_UPLOAD_FILE']['name']);
			if ($error != null)
			{
				Response::error($error);//make a red box around the componant instead. Like TextFields
			}
			$this->refreshInRow($this->r());
			if ($this->afterValueChangeE != null)
			{
				eval($this->afterValueChangeE);
			}
		}
		
		function download($linkId)
		{
			$this->setContextParam('row',$this->r()->getView()->getRowByLinkId($linkId));
			Response::addMessage('setattr',$this->getFullId().",urltodownload,".$this->r()->getValue($this->c("col"))->getUrl($this->r()->getValue($this->c("col_name"))));
		}
		
		function afterValueChangeE($e)
		{
			$this->afterValueChangeE = $e;
		}
		
		
		function setEditableE($e)
		{
			$this->editableE = $e;
		}
		
		function setUploadRenderedE($e)
		{
			$this->uploadRenderedE = $e;
		}
		
		function setUploadRendered($v)
		{
			$this->uploadRenderedE = 'return '.$v.';';
		}
		
		function setDownloadRenderedE($e)
		{
			$this->downloadRenderedE = $e;
		}
		
		function setDownloadRendered($v)
		{
			$this->downloadRenderedE = 'return '.$v.';';
		}
	}
	
	if (isset ($_POST['FILE_UPLOAD']) && isset($_FILES['FILE_UPLOAD_FILE']))
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
		
		$s = explode('x',$_POST['FILE_UPLOAD'],4);
		Application::getApplication($s[1])->getComponent($s[2])->fileChange($s[3],$_FILES['FILE_UPLOAD_FILE']['tmp_name'],$_FILES['FILE_UPLOAD_FILE']['name']);
		Response::send();
	}
	if (isset ($_POST['FILE_DOWNLOAD']))
	{
		require $_SERVER['DOCUMENT_ROOT']."/skylin/InitRequest.php";
		
		$s = explode('x',$_POST['FILE_DOWNLOAD'],4);
		Application::getApplication($s[1])->getComponent($s[2])->download($s[3]);
		Response::send();
	}
?>