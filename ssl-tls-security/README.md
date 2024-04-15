# ssl-tls-security

NOT YET IMPLEMENTED

An example Kafka project showcasing Kafka's support for SSL/TLS security.

---
**NOTE**: Although TLS (Transport Layer Security) has replaced SSL completely, the SSL name lives on. I've included "ssl-"
in the name of this subproject because the name SSL is still so common.

---

Dealing with certificates can be a bit of a mystery to most engineers. I'd like to de-mystify it with a practical example:
a Kafka producer and consumer using TLS!

## Notes

Miscellaneous notes and commands:

* http://kafka.apache.org/documentation/#security
* Using Java 11
* `keytool -keystore generated.pk12 -alias localhost -validity 3650 -genkey -keyalg RSA -storetype pkcs12`
  * Can I create the keystore without all the interactive prompts for things like name and country?
* https://stackoverflow.com/questions/4907622/keytool-see-the-public-and-private-keys#comment101625804_45891165
* `openssl pkcs12 -in generated.pk12 -nocerts -nodes`
* `openssl pkcs12 -in generated.pk12 -nodes`
* Observation: pkcs12 seems to be the modern way. Avoid jks files and prefer pkcs12. Commonly ending with the file extension
  `.pk12` (I think?)?  
* Can I open a pkcs12 file with [keystore-explorer](https://github.com/kaikramer/keystore-explorer)?
* What is PKCS11 and why does keystore-explorer have an "Open Special" option for it? Does it not know about PKCS12?
* Whoa, keystore-explorer can detect the type of a file! For example, it detected that the my `generated.pk12` file was
  "PKCS #12 KeyStore". And when I renamed it to something with a misleading file extension, like `generated.txt`, it
  still detected that it was PKCS #12!
* Shoot, keystore-explorer cant' inspect PKCS 12 files? I got the error
  > org.kse.crypto.keystore.KeyStoreLoadException: Could not load KeyStore as type 'PKCS12'
* **Concept: certificate chains**: <https://en.wikipedia.org/wiki/X.509#Certificate_chains_and_cross-certification> 
  * What does the "Subject" and "Issuer" fields mean in a certificate?
* What is the difference between a certificate and a private key? I know private keys are private and certificates are
  public but I mean what actually *is* a key and what actually *is* a certificate and how are they different?
  Answer:
  > A TLS public key is distributed in a file called a certificate. It is called a certificate because it is certified to
  > contain a valid public key by a digital signature. There are a few different techniques used to sign the certificates used by sendmail.
  -- from [*Sendmail Cookbook*](https://learning.oreilly.com/library/view/sendmail-cookbook/0596004710/ch08.html)
* > This is because several CA certificates can be generated for the same subject and public key, but be signed with different
  > private keys (from different CAs or different private keys from the same CA).
  What does this mean?
* > At the bottom of the display is the digital signature of the certificate created by the CA. It is an MD5 digest of the
  > certificate, which is stored in the sendmail configuration in the macro ${cert_md5}. The digest is encrypted by the CA’s
  > private key. sendmail uses the CA’s public key to decrypt the digest. If sendmail successfully decrypts and verifies
  > the digest, it stores OK in the macro ${verify} and considers the certificate valid for authentication.
  What does this mean?

## Instructions

* Install Kafka and `kcat`:
    * `brew install kafka`
    * `brew install kcat`
* Create the certificate objects (warning: this process is pretty involved!):
    * Note: the following instructions are mostly derived from the [Apache Kafka docs on security](http://kafka.apache.org/documentation/#security)
    * Create a root certificate (you will see this referred to as a CA using a homegrown PKI)
      * ```
        openssl req -x509 \
        -new \
        -keyout root-private-key.pem \
        -out root-cert.pem \
        -subj "/C=US/ST=mystate/L=mycity /O=myorgOU=myorgunit/CN=myname/emailAddress=myemail"
        ```
      * It will prompt you to create a password. Create one.        
      * Command explanation: the `req` sub-command of the `openssl` command is designed to create certificate signing
        requests (CSRs) but when the `-x509` option is given it creates a self-signed cert. We will use this as our root
        certificate, or "trust anchor" from which all other components of our PKI will assume trust.  
    * Create a PKCS12-style Java keystore intended for the Kafka broker with the following command:
    * ```
      keytool \
        -keystore kafka-broker-keystore-unsigned.pk12 \
        -alias localhost \
        -validity 3650 \
        -genkey \
        -keyalg RSA \
        -storetype pkcs12 \
        -dname "cn=myname, ou=myorgunit, o=myorg, c=US"
      ```
    * TODO Why use `keytool` for this? I would prefer to stick with `openssl` because that is a more transferable tool. After
      all, if I'm setting up TLS things for apps based in GoLang then I won't have `keytool` because it's a Java tool. Is
      there a unique advantage to using `keytool` or does it just provide some convenience for Java developers?
      Alternatively, can we come up with a combination of `openssl` commands that do the same thing that the `keytool`
      command does? Let's find out below:
      * Create a key-pair (I think you have to create it into separate files: the private key and the certificate. Later,
        you can combine it into a PKCS12 archive file. `keytool` does this in one command not two!)
        ```
        openssl req -x509 \
        -new \
        -keyout kafka-broker-private-key.pem \
        -out kafka-broker-cert-unsigned.pem \
        -nodes \
        -subj "/C=US/ST=mystate/L=mycity /O=myorgOU=myorgunit/CN=mykafkabrokername/emailAddress=myemail"                
        ```
        Explanation of command: `-nodes` is short for "no DES" and it means it won't generate a password for the private
        key.
      * WIP Next, combine them into a PKCS12 file which is needed (I think?) to create the certificate signing request.
        ```
        openssl pkcs12 \
        -export \
        -out kafka-broker-keystore-unsigned.pk12 \
        -inkey kafka-broker-private-key.pem \
        -in kafka-broker-cert-unsigned.pem \
        -passout pass:
        ```
        Explanation of command: `-passout pass:` means that the created PKCS12 will have a blank password (or no password?)
        protecting the secret key. See [this StackOverflow answer](https://stackoverflow.com/a/56552040).
    * It will prompt you to create a password. Create one.
    * As is, this cert is not useful because it is not signed by the root certificate. Remember, the root certificate is
      the only thing we trust implicitly (it is a "trust anchor") while the Kafka broker's certificate will be trusted
      thanks, to math! We *derive* trust in the Kafka broker's cert because it will be signed by the root cert. 
* Start Kafka (TODO enable SSL):
    * `./scripts/start-kafka.sh`
* Start a Kafka consumer:
    * TODO
* Produce a message:
    * TODO
    * Verify that you see it in the consumer!
    * Success!
* Stop Kafka:
    * `./scripts/stop-kafka.sh`

## Reference Materials

* [Wikipedia: *Public Key Infrastructure*](https://en.wikipedia.org/wiki/Public_key_infrastructure)
* [Wikipedia: *Public key certificate*](https://en.wikipedia.org/wiki/Public_key_certificate)
* [Wikipedia: *Certification path validation algorithm*](https://en.wikipedia.org/wiki/Certification_path_validation_algorithm)
  * This is nice but how do digital signatures actually work?
  * What is a "trust anchor"? Is it the first certificate a "relying party" (e.g. a web browser) sees from the web server
    it is visiting? And then the path validation algorithm validates this anchor and then all the "parent" (my word) certificates
    until it gets to a root certificate?
* [Wikipedia: *Digital signature*](https://en.wikipedia.org/wiki/Digital_signature)
* [Wikipedia: *Trust anchor*](https://en.wikipedia.org/wiki/Trust_anchor)
  > In cryptographic systems with hierarchical structure, a trust anchor is an authoritative entity for which trust is assumed and not derived.
* [Wikipedia: *Public-key cryptography*](https://en.wikipedia.org/wiki/Public-key_cryptography)
  * **This is the magic sauce that backs all the other topics in PKI**. Public-key cryptography (rather, asymmetric cryptography)
    is the essential concept to understand while topics like PKCS, PEM, and Java's keystore technology (JKS) are all tangential
    tools and trivia that make use of public-key cryptography. 
* [Wikipedia: *X.509*](https://en.wikipedia.org/wiki/X.509)
* [Wikipedia: *PKCS*](https://en.wikipedia.org/wiki/PKCS)
* [Wikipedia: *PKCS 8*](https://en.wikipedia.org/wiki/PKCS_8)
* [Wikipedia: *Privacy-Enhanced Mail*](https://en.wikipedia.org/wiki/Privacy-Enhanced_Mail)
* [GitHub repo: `keystore-explorer`](https://github.com/kaikramer/keystore-explorer)
* [Blog post on Kafka SSL](https://medium.com/analytics-vidhya/kafka-ssl-encryption-authentication-part-two-practical-example-for-implementing-ssl-in-kafka-d514f30fe782)
* [Book (available on Oreilly): *Network Security Assesment, 3rd Edition. Chapter 11. Assessing TLS Services*](https://learning.oreilly.com/library/view/network-security-assessment/9781491911044/ch11.html#assessing_tls_services)
* [Apache Kafka docs: *Security*](http://kafka.apache.org/documentation/#security)
