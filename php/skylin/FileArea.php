<?php
require_once "Component.php";
/**
 * A drop area for files and folders. Every file dropped on it becomes a new row of the view: the file name in the name
 * column and the file in the blob column (skylin/file-upload-worker.js posts each file to FileUpload through
 * restless.php). When all files are in, afterUploadE runs.
 * Every render asks the view for a new upload grant (View.allowFileUpload) on these two columns and writes its token into
 * the page; FileUpload accepts only a request that sends the current token, and takes the columns from the grant. A
 * render (F5, or a refresh of the area or anything around it) therefore stops uploads that are still going.
 * A render while the area is not editable or not rendered, or draws no view or another view, withdraws the grant.
 * With setChild the area draws that component (a Table of the view, say); without one it lists the file names itself.
 */
class FileArea extends Component
{
	private $viewE;
	private $view;
	private $blobCol;
	private $nameCol;
	private $afterUploadE;
	private $editableE = 'return true;';
	private $child;
	private $grantedViews = array();
	private $drawn;

	public function __construct($blobCol = null,$nameCol = null)
	{
		parent::__construct();
		$this->blobCol = $blobCol;
		$this->nameCol = $nameCol;
	}

	function setView($view)
	{
		$this->view = $view;
		return $this;
	}

	function setViewE($e)
	{
		$this->viewE = Expression::toExpression($e);
		return $this;
	}

	function setEditableE($e)
	{
		$this->editableE = Expression::toExpression($e);
		return $this;
	}

	function setEditable($e)
	{
		$this->editableE = 'return '.$e.';';
		return $this;
	}

	function isEditable()
	{
		return (bool)eval($this->editableE);
	}

	function renderInContext($context)
	{
		$this->drawn = false;
		$ret = parent::renderInContext($context);
		if (!$this->drawn)
		{
			$this->revokeGrant($this->getFullId());
		}
		return $ret;
	}

	function render()
	{
		$this->drawn = true;
		if (!is_null($this->viewE))
		{
			try
			{
				$this->view = eval($this->viewE);
			}
			catch (Throwable $e)
			{
				$this->view = null;
			}
		}
		$class = 'file_area';
		$files = '';
		if ($this->child != null)
		{
			$context = $this->getContext();
			$context['parent'] = $this;
			$files = $this->child->renderInContext($context);
		}
		else
		{
			$class = $class.' file_area_standalone';
			if ($this->view != null)
			{
				foreach ($this->view->getRows() as $row)
				{
					$files = $files.'<div class="file_area_uploaded_text">'.Component::escapeHtml($row->getValue($this->nameCol)).'</div>';
				}
			}
		}
		$viewId = '';
		$token = '';
		$owner = $this->getFullId();
		$open = $this->view != null && $this->isEditable();
		$previous = $this->grantedViews[$owner] ?? null;
		if ($previous != null && (!$open || $previous->_getJavaObjectId() != $this->view->_getJavaObjectId()))
		{
			$this->revokeGrant($owner);
		}
		if ($this->view != null)
		{
			$viewId = $this->view->_getJavaObjectId();
		}
		if ($open)
		{
			$token = (string)$this->view->allowFileUpload($owner,$this->nameCol,$this->blobCol);
			$this->grantedViews[$owner] = $this->view;
		}
		else
		{
			$class = $class.' file_area_disabled';
		}
		$style = is_null($this->getStyle()) ? '' : ' style="'.$this->getStyle().'"';
		return '<div id="'.$owner.'" viewid="'.Component::escapeHtml($viewId).'" uploadtoken="'.Component::escapeHtml($token).'" class="'.$this->getClasses($class).'"'.$style.'>'.$files.'</div>';
	}

	private function revokeGrant($owner)
	{
		$previous = $this->grantedViews[$owner] ?? null;
		if ($previous != null)
		{
			$previous->revokeFileUpload($owner);
		}
		unset($this->grantedViews[$owner]);
	}

	function upload($linkId)
	{
		$view = $this->r()->getView();
		if ($view != null)
		{
			$this->setContextParam('row',$view->getRowByLinkId($linkId));
		}
		if (!$this->getRendered() || !$this->isEditable())
		{
			return;
		}
		if (!is_null($this->afterUploadE))
		{
			eval($this->afterUploadE);
		}
	}

	function afterUploadE($e)
	{
		$this->afterUploadE = Expression::toExpression($e);
		return $this;
	}

	function setChild($c)
	{
		$this->child = $c;
		return $c;
	}
}

if (isset($_POST['FILE_AREA_UPLOAD']))
{
	require "InitRequest.php";
	$s = explode('x',$_POST['FILE_AREA_UPLOAD'],4);
	$com = Application::getApplication($s[1])->getComponent($s[2]);
	if ($com instanceof FileArea)
	{
		$com->upload($s[3]);
	}
	Response::send();
}
?>
