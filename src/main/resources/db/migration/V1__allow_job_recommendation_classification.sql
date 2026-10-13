DO $migration$
BEGIN
    IF to_regclass('public.processed_gmail_messages') IS NOT NULL THEN
        ALTER TABLE public.processed_gmail_messages
            DROP CONSTRAINT IF EXISTS processed_gmail_messages_classification_check;

        ALTER TABLE public.processed_gmail_messages
            ADD CONSTRAINT processed_gmail_messages_classification_check
            CHECK (
                classification IS NULL OR classification IN (
                    'REJECTED',
                    'OFFER',
                    'INTERVIEW',
                    'ONLINE_ASSESSMENT',
                    'PROGRESSED',
                    'APPLICATION_RECEIVED',
                    'PENDING',
                    'ROLE_CLOSED',
                    'WITHDRAWN',
                    'REVIEW_REQUIRED',
                    'JOB_RECOMMENDATION',
                    'OTHER'
                )
            );
    END IF;
END
$migration$;
