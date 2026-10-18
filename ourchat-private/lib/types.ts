export interface Member { user_id: string; display_name: string; created_at?: string }
export interface ChatMessage { id: string; sender_id: string; body: string; client_nonce: string; created_at: string }
export interface ReadReceipt { user_id: string; last_read_message_id: string | null; updated_at: string }
