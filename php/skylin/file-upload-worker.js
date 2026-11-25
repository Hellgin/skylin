// Uploads the files a FileArea received, one at a time: each one is a row added to the same view.
// The page posts {url, header, files}; header is "<view id>_<upload token>_" and the file name is appended
// URL-encoded, as FileUpload expects. FileUpload answers "stored" for a file it kept and nothing for one it refused,
// which is what happens to every file once the area renders again and its token is replaced, so the first failure
// ends the upload. The page hears {type:"uploaded", index, ok} per file sent and {type:"done"} at the end.
self.onmessage = async function (msg)
{
	var data = msg.data;
	for (var i = 0; i < data.files.length; i++)
	{
		var file = data.files[i];
		var ok = false;
		try
		{
			var res = await fetch(data.url, {method: 'POST',
											 credentials: 'same-origin',
											 headers: {'Authorization': data.header + encodeURIComponent(file.name)},
											 body: file});
			ok = res.ok && (await res.text()) === 'stored';
		}
		catch (e)
		{
		}
		postMessage({type: 'uploaded', index: i, ok: ok});
		if (!ok)
		{
			break;
		}
	}
	postMessage({type: 'done'});
};
