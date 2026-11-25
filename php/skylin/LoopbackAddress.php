<?php
	// Whether an address is this machine's own: 127.0.0.0/8, ::1, or 127.x.x.x written as an IPv4-mapped IPv6 address.
	// Pass REMOTE_ADDR, the address of the TCP connection. That is only the caller's address while nothing in front of
	// PHP (mod_remoteip, a proxy on the same machine) replaces it.
	class LoopbackAddress
	{
		static function is($address)
		{
			if (!is_string($address) || filter_var($address, FILTER_VALIDATE_IP) === false)
			{
				return false;
			}
			$packed = inet_pton($address);
			if (strlen($packed) == 16)
			{
				if ($packed === inet_pton('::1'))
				{
					return true;
				}
				if (substr($packed,0,12) !== "\0\0\0\0\0\0\0\0\0\0\xff\xff")
				{
					return false;
				}
				$packed = substr($packed,12);
			}
			return strlen($packed) == 4 && ord($packed[0]) == 127;
		}
	}
?>
