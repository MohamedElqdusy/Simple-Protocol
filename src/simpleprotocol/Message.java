package simpleprotocol;

import java.util.Random;

/*
 * Only one P0P message may be sent in a single UDP packet .
 * P0P itself does not define either maximum or minimum DATA payload sizes .
 */
public class Message {
    public Message(){
        magic = 0xC461 ; // i.e., decimal 50273, if taken as an unsigned 16-bit integer
        version = 1;
        command = 0 ;
        sequence = 0;

        Random rand = new Random();
        session = "0x"+Integer.toHexString(rand.nextInt());      
    }
        /*
        *    magic****version***command***sequence_number****session id
        *    16 bits  8 bits    8 bits      32 bits           32 bits
        */
       public int magic;   
       public byte version; // 8-bit
        /*
         * command is : 
         * 0 for HELLO, 
         * 1 for DATA, 
         * 2 for ALIVE, 
         * 3 for GOODBYE.
         */
       public byte command ; // 8-bit
        
        /* 
         * 32-bit
         * sequence numbers in messages sent by the client are :
         * 0 for the first packet of the session, 
         * 1 for the next packet, 
         * 2 for the one after that, etc.
         */
       public int sequence ;
              
        /*
         * session id is an arbitrary 32-bit integer. 
         * The client chooses its value at random when it starts. 
         * Both the client and the server repeat the session id bits 
         * in all messages that are part of that session.
         */       
        public String session ;
        
        /*
         * Message Data payload
         */
        public String DataPayload ;
        
        
//convert Message Header and Data payload to String
        @Override
        public String toString(){
            String str ;
            str = String.format("%s%s%s%s [%s] %s",magic,version,command,session,sequence,DataPayload);
            return str;
        }

}
