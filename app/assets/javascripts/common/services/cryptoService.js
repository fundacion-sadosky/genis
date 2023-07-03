define([ 'cryptojs-enc', 'cryptojs-aes' ], function(enc, aes) {
'use strict';

function CryptoService(userService) {
	
	this.encryptBase64 = function(text) {

		var user = userService.getUser();
		
		var keyTxt = user.credentials.key;
		var ivTxt = user.credentials.iv;
		
		var key = enc.enc.Hex.parse(keyTxt);
		var iv = enc.enc.Hex.parse(ivTxt);

		var encryptedBytes = aes.AES.encrypt(text, key, {iv : iv});

		var encryptedText = encryptedBytes
			.ciphertext
			.toString(enc.enc.Base64)
			.replace(/\//g, '_')
			.replace(/\+/g, '-')
			.replace(/=/g, '');
		
		return encryptedText;

	};

	this.encryptRequest = function(request) {

		var user = userService.getUser();

		request.headers['X-USER'] = user.name;
		request.headers['X-SUPERUSER'] = user.superuser;
		
		request.url = this.encryptBase64(request.url);

		if (request.data) {
			request.data = this.encryptBase64(JSON.stringify(request.data));
		}

		return request;
	};

}

return CryptoService;

});
