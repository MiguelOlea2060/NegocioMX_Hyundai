package com.example.negociomx_hyundai.BLL

import org.apache.commons.codec.binary.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class BLLCifrado {
    val secretKey = "tK5UTui+DPh8lIlBxya5XVsmeDCoUl6vHhdIESMB6sQ="
    val salt = "QWlGNHNhMTJTQWZ2bGhpV3U=" // base64 decode => AiF4sa12SAfvlhiWu
    val iv = "bVQzNFNhRkQ1Njc4UUFaWA==" // base64 decode => mT34SaFD5678QAZX

    fun cifrar(cadena:String):String?
    {
        val secretKeys=SecretKeySpec(cadena.toByteArray(),"AES")
        val iv=ByteArray(16)
        val charArray=cadena.toCharArray()
        for (i in 0 until charArray.size)
            iv[i]=charArray[i].toByte()

        val ivParametros=IvParameterSpec(iv)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,secretKeys,ivParametros)

        val valorCifrado=cipher.doFinal(cadena.toByteArray())
        return Base64.encodeBase64String(valorCifrado)
    }

    fun descifrar(cadena:String):String?
    {
        val secretKeySpec = SecretKeySpec(cadena.toByteArray(), "AES")
        val iv = ByteArray(16)
        val charArray = cadena.toCharArray()
        for (i in 0 until charArray.size){
            iv[i] = charArray[i].toByte()
        }
        val ivParameterSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val decryptedByteValue = cipher.doFinal(Base64.decodeBase64(cadena))
        return String(decryptedByteValue)
    }

}