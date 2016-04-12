package com.karasiq.nanoboard.captcha.internal

import java.util.function.Supplier

import akka.util.ByteString
import net.i2p.crypto.eddsa.spec.{EdDSANamedCurveTable, EdDSAPrivateKeySpec, EdDSAPublicKeySpec}
import net.i2p.crypto.eddsa.{EdDSAEngine, EdDSAPrivateKey, EdDSAPublicKey}

/**
  * Ed25519 digital signature utility
  */
private[captcha] object Ed25519 {
  /**
    * Default curve specification
    */
  private val curve25519 = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.CURVE_ED25519_SHA512)

  private val tlEngine = ThreadLocal.withInitial(new Supplier[EdDSAEngine] {
    override def get(): EdDSAEngine = new EdDSAEngine()
  })

  /**
    * Reads encoded public key
    * @param data Public key data
    * @return EdDSA public key
    */
  def publicKey(data: ByteString): EdDSAPublicKey = {
    new EdDSAPublicKey(new EdDSAPublicKeySpec(data.toArray[Byte], curve25519))
  }

  /**
    * Reads private key from seed
    * @param seed Private key 32-byte seed
    * @return EdDSA private key
    */
  def privateKey(seed: ByteString): EdDSAPrivateKey = {
    new EdDSAPrivateKey(new EdDSAPrivateKeySpec(seed.toArray[Byte], curve25519))
  }

  /**
    * Verifies EdDSA digital signature with the provided key
    * @param key EdDSA public key
    * @param data Signed data
    * @param signature EdDSA digital signature
    * @return Is signature valid
    */
  def verify(key: EdDSAPublicKey, data: ByteString, signature: ByteString): Boolean = {
    val engine = tlEngine.get()
    engine.initVerify(key)
    engine.verifyOneShot(data.toArray, signature.toArray[Byte])
  }

  /**
    * Signs data with the provided key
    * @param key EdDSA private key
    * @param data Data to sign
    * @return EdDSA digital signature
    */
  def sign(key: EdDSAPrivateKey, data: ByteString): ByteString = {
    val engine = tlEngine.get()
    engine.initSign(key)
    ByteString(engine.signOneShot(data.toArray))
  }
}
