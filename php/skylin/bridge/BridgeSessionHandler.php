<?php
/**
 * PHP's session handler, kept by the JVM (bridge command b, Java BridgeSessionHandler).
 * The session id comes from the browser's cookie and becomes part of a file name in the JVM, so only ids of 22 to 128
 * letters, digits and '-' are used (isValidId, checked again in Java). install() turns on session.use_strict_mode, so
 * PHP asks validateId about an id from a cookie and replaces one the JVM does not know with a new id from create_sid.
 */
class BridgeSessionHandler implements SessionHandlerInterface, SessionIdInterface, SessionUpdateTimestampHandlerInterface
{
    private $savePath;
    private $bridgeConnection;

    function __construct($con)
    {
    	$this->bridgeConnection = $con;
    }

    /** Uses this handler for the sessions of the request. Call it before session_start(). */
    static function install($con)
    {
    	ini_set('session.use_strict_mode', '1');
    	session_set_save_handler(new BridgeSessionHandler($con), true);
    }

    static function isValidId($id)
    {
    	return is_string($id) && preg_match('/^[A-Za-z0-9-]{22,128}$/D', $id) === 1;
    }
    
    public function open($savePath, $sessionName): bool
    {
    	$this->savePath = $savePath;
    	//return ((string)jvmCall('b,1,'.$savePath.','.session_id().','.$sessionName)) === 'true';
    	$_REQUEST['USING_BRIDGE_SESSION_HANDLER'] = true;
    	//PHP opens before it checks the id; an id that is not valid is replaced next (validateId), so the JVM gets none
    	$id = self::isValidId(session_id()) ? session_id() : '';
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,1,'.$savePath.','.$id.','.$sessionName)) === 'true';
    }
    
    public function read($id): string|false
    {
    	if (!self::isValidId($id))
    	{
    		return false;
    	}
    	//return (string)jvmCall('b,2,'.$id);
    	return (string)$this->bridgeConnection->jvmCall_internal('b,2,'.$id);
    }

    public function write($id, $data): bool
    {
    	if (!self::isValidId($id))
    	{
    		return false;
    	}
    	//return ((string)(new BridgeConnection())->jvmCall_internal('b,3,'.$id.','.$data)) === 'true';
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,3,'.$id.','.$data)) === 'true';
    	//return (string)jvmCall('b,3,'.$id.','.$data);
    }
    
    public function close(): bool
    {
    	if (!self::isValidId(session_id()))
    	{
    		return true;
    	}
    	//return ((string)(new BridgeConnection())->jvmCall_internal('b,4,'.session_id())) === 'true';
    	$ret = ((string)$this->bridgeConnection->jvmCall_internal('b,4,'.session_id())) === 'true';
    	//$this->bridgeConnection->jvmCall_internal('6');
    	return $ret;
    	//return (string)jvmCall('b,4,'.session_id());
    }



    public function destroy($id): bool
    {
    	if (!self::isValidId($id))
    	{
    		return false;
    	}
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,5,'.$id)) === 'true';
    }

    public function gc($maxlifetime): int|false
    {
    	return ((string)$this->bridgeConnection->jvmCall_internal('b,6,'.$maxlifetime)) === 'true' ? 0 : false;
    }

    /** A new id: 128 random bits as 32 hex digits, whatever session.sid_length and sid_bits_per_character say. */
    public function create_sid(): string
    {
    	return bin2hex(random_bytes(16));
    }

    /** session.use_strict_mode: an id from a cookie is used only when it is valid and the JVM knows the session. */
    public function validateId($id): bool
    {
    	return self::isValidId($id) && ((string)$this->bridgeConnection->jvmCall_internal('b,7,'.$id)) === 'true';
    }

    public function updateTimestamp($id, $data): bool
    {
    	return $this->write($id, $data);
    }
}
?>