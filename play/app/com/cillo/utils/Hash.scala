package com.cillo.utils

object Hash {
    val defaultAlphabet: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    val defaultSeps: String = "cfhistuCFHISTU"

    def apply(
                 salt: String = "",
                 minHashLength: Int = 0,
                 alphabet: String = defaultAlphabet,
                 seps: String = defaultSeps): Hash = {
        val sepDiv: Double = 3.5
        val guardDiv: Int = 12
        val minAlphabetLength: Int = 16
        var guards: String = ""
        val myMinHashLength = minHashLength max 0

        val distinctAlphabet = alphabet.distinct

        if (distinctAlphabet.length < minAlphabetLength) {
            throw new IllegalArgumentException(s"alphabet must contain at least $minAlphabetLength unique characters")
        }

        if (distinctAlphabet.contains(" ")) {
            throw new IllegalArgumentException("alphabet cannot contains spaces")
        }

        // seps should contain only characters present in alphabet
        // alphabet should not contains seps
        var myAlphabet = distinctAlphabet diff seps
        var mySeps = distinctAlphabet intersect seps

        myAlphabet = myAlphabet.replaceAll("\\s+", "")
        mySeps = mySeps.replaceAll("\\s+", "")
        mySeps = consistentShuffle(mySeps, salt)

        if (mySeps == "" || (myAlphabet.length / mySeps.length) > sepDiv) {
            var seps_len: Int = (myAlphabet.length / sepDiv).ceil.toInt

            if (seps_len == 1) {
                seps_len += 1
            }

            if (seps_len > mySeps.length) {
                val diff = seps_len - mySeps.length
                mySeps += myAlphabet.take(diff)
                myAlphabet = myAlphabet.drop(diff)
            } else {
                mySeps = mySeps.take(seps_len)
            }
        }

        myAlphabet = consistentShuffle(myAlphabet, salt)

        val guardCount = (myAlphabet.length.toDouble / guardDiv).ceil.toInt

        if (myAlphabet.length < 3) {
            guards = mySeps.take(guardCount)
            mySeps = mySeps.drop(guardCount)
        } else {
            guards = myAlphabet.take(guardCount)
            myAlphabet = myAlphabet.drop(guardCount)
        }

        new Hash(
            salt,
            myMinHashLength,
            myAlphabet,
            mySeps,
            guards)
    }

    private def consistentShuffle(alphabet: String, salt: String): String = {
        if (salt.length <= 0) {
            return alphabet
        }

        val as = alphabet.toCharArray
        var p = 0

        for (i <- (as.length - 1) until 0 by -1) {
            val v = (as.length - 1 - i) % salt.length
            val asc_val = salt(v).toInt
            p += asc_val
            val j = (asc_val + v + p) % i

            val tmp = as(j)
            as(j) = as(i)
            as(i) = tmp
        }

        return new String(as)
    }

    private def hash(input: Long, alphabet: String): String = {
        var hash = ""
        val alphabetLen = alphabet.length
        var myInput = input

        do {
            hash = alphabet((myInput % alphabetLen).toInt) + hash
            myInput /= alphabetLen
        } while (myInput > 0)

        return hash
    }

    private def unhash(input: String, alphabet: String): Long = {
        var number = 0L

        for (i <- 0 until input.length) {
            val pos = alphabet.indexOf(input(i))
            number += (pos * scala.math.pow(alphabet.length, input.length - i - 1)).toLong
        }

        return number
    }
}

class Hash private (
                          salt: String,
                          minHashLength: Int,
                          alphabet: String,
                          seps: String,
                          guards: String) {
    /**
     * Encode numbers to string
     *
     * @param numbers the numbers to encrypt
     * @return the encrypt string
     */
    def encode(numbers: Long*): String = {
        if (numbers.length == 0) {
            return ""
        }

        return this._encode(numbers: _*)
    }

    /**
     * Decode string to numbers
     *
     * @param hash the encrypt string
     * @return decryped numbers
     */
    def decode(hash: String): Seq[Long] = {
        if (hash == "")
            return Seq.empty

        return this._decode(hash, this.alphabet)
    }

    /**
     * Encode string representation of hexadecimal number to hashid.
     *
     * @param hexa the hexa to encrypt
     * @return the encrypt string
     */
    def encodeHex(hexa: String): String = {
        if (!hexa.matches("^[0-9a-fA-F]+$")) {
            ""
        } else {
            var matched = List.empty[Long]
            val matcher = java.util.regex.Pattern.compile("[\\w\\W]{1,12}").matcher(hexa)

            while (matcher.find()) {
                val group = matcher.group()
                matched = java.lang.Long.parseLong("1" + group, 16) :: matched
            }

            this._encode(matched.reverse: _*)
        }
    }

    /**
     * Decode hashid string to hex string.
     *
     * @param hash the encrypt string
     * @return decryped numbers
     */
    def decodeHex(hash: String): String = {
        var result = "";
        val numbers = this.decode(hash)

        for (number <- numbers) {
            result += java.lang.Long.toHexString(number).substring(1).toUpperCase
        }

        return result
    }

    private def _encode(numbers: Long*): String = {
        var numberHashInt = 0

        for (i <- 0 until numbers.size) {
            numberHashInt += (numbers(i) % (i + 100)).toInt
        }

        var alphabet = this.alphabet
        val ret = alphabet(numberHashInt % alphabet.length)
        val lottery = ret
        var ret_str = ret + ""

        for (i <- 0 until numbers.length) {
            var num = numbers(i)
            val buffer = lottery + this.salt + alphabet

            alphabet = Hash.consistentShuffle(alphabet, buffer.take(alphabet.length))
            val last = Hash.hash(num, alphabet)

            ret_str += last

            if (i + 1 < numbers.length) {
                num %= (last(0) + i).toInt
                val sepsIndex = (num % this.seps.length).toInt
                ret_str += this.seps(sepsIndex)
            }
        }

        if (ret_str.length < this.minHashLength) {
            var guardIndex = (numberHashInt + ret_str(0).toInt) % this.guards.length
            var guard = this.guards(guardIndex)

            ret_str = guard + ret_str

            if (ret_str.length < this.minHashLength) {
                guardIndex = (numberHashInt + ret_str(2).toInt) % this.guards.length
                guard = this.guards(guardIndex)

                ret_str += guard
            }
        }

        val halfLen = alphabet.length / 2
        while (ret_str.length < this.minHashLength) {
            alphabet = Hash.consistentShuffle(alphabet, alphabet)
            ret_str = alphabet.drop(halfLen) + ret_str + alphabet.take(halfLen)
            val excess = ret_str.length - this.minHashLength
            if (excess > 0) {
                val start_pos = excess / 2
                ret_str = ret_str.substring(start_pos, start_pos + this.minHashLength)
            }
        }

        return ret_str
    }

    private def _decode(hash: String, alphabet: String): Seq[Long] = {
        var myAlphabet = alphabet
        var ret = List.empty[Long]

        var i = 0
        var hashBreakdown = hash.map(c => if (guards.contains(c)) ' ' else c)
        var hashArray = hashBreakdown.split(" ")

        if (hashArray.length == 3 || hashArray.length == 2) {
            i = 1
        }

        hashBreakdown = hashArray(i)

        val lottery = hashBreakdown(0)
        hashBreakdown = hashBreakdown.drop(1)
        hashBreakdown = hashBreakdown.map(c => if (seps.contains(c)) ' ' else c)
        hashArray = hashBreakdown.split(" ")

        for (aHashArray <- hashArray) {
            val subHash = aHashArray
            val buffer = lottery + this.salt + myAlphabet
            myAlphabet = Hash.consistentShuffle(myAlphabet, buffer.take(myAlphabet.length))
            ret ::= Hash.unhash(subHash, myAlphabet)
        }

        val seq = ret.reverse

        if (encode(seq: _*) == hash) seq else Seq.empty
    }

}